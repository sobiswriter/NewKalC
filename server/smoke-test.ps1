param([switch]$Private, [switch]$Meal, [string]$NonFoodImage)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true
$serverUrl = (gcloud run services describe kalc-vertex --project=gen-lang-client-0100408368 --region=us-central1 --format='value(status.url)').Trim()
$headers = @{}
if ($Private) {
    $identityToken = (gcloud auth print-identity-token).Trim()
    $headers['X-Serverless-Authorization'] = "Bearer $identityToken"
}
function Send-Request($route, $data, $session = '') {
    $requestHeaders = @{} + $headers
    if ($session) { $requestHeaders['Authorization'] = "Bearer $session" }
    $timer = [Diagnostics.Stopwatch]::StartNew()
    $response = Invoke-WebRequest "$serverUrl/$route" -Method Post -Headers $requestHeaders -ContentType 'application/json' -Body ($data | ConvertTo-Json -Depth 15 -Compress) -SkipHttpErrorCheck
    Write-Host "$route completed in $([Math]::Round($timer.Elapsed.TotalSeconds, 2))s"
    return $response
}
function Assert-Status($response, $expected, $label) {
    if ($response.StatusCode -ne $expected) { throw "$label failed: expected $expected, received $($response.StatusCode)." }
    Write-Output "$label : HTTP $expected"
}
$wrong = Send-Request 'session' @{ passkey = 'wrong-passkey-smoke-test' }
Assert-Status $wrong 401 'Wrong passkey denied'
$anonymous = Send-Request 'generate' @{}
Assert-Status $anonymous 401 'Anonymous generation denied'
$passkey = [IO.File]::ReadAllText((Join-Path $env:LOCALAPPDATA 'KalC/server-passkey.txt')).TrimEnd()
$sessionResponse = Send-Request 'session' @{ passkey = $passkey }
$passkey = $null
Assert-Status $sessionResponse 200 'Correct passkey accepted'
$session = ($sessionResponse.Content | ConvertFrom-Json).token
$request = @{
    contents = @(@{ parts = @(@{ text = 'Reply with only the word OK.' }) })
    generationConfig = @{ temperature = 0; maxOutputTokens = 64 }
}
$answer = Send-Request 'generate' $request $session
Assert-Status $answer 200 'Authorized Vertex generation'
$parsed = $answer.Content | ConvertFrom-Json
if (-not $parsed.candidates[0].content.parts) { throw 'Missing generated content.' }
if ($Meal) {
    $source = [IO.File]::ReadAllText((Join-Path $PSScriptRoot '../app/src/main/java/com/example/data/gemini/GeminiFoodService.kt'))
    $prompts = [regex]::Matches($source, '(?s)val prompt = """\r?\n(.*?)\r?\n"""\.trimIndent\(\)')
    $image = [Convert]::ToBase64String([IO.File]::ReadAllBytes((Join-Path $PSScriptRoot '../docs/images/sample_thali.jpg')))
    foreach ($index in @(0, 1)) {
        $prompt = $prompts[$index].Groups[1].Value.Replace('$validationThreshold', '0.65')
        $payload = @{
            contents = @(@{ parts = @(@{ text = $prompt }, @{ inlineData = @{ mimeType = 'image/jpeg'; data = $image } }) })
            generationConfig = @{ responseMimeType = 'application/json'; maxOutputTokens = 1024 }
        }
        $photo = Send-Request 'generate' $payload $session
        Assert-Status $photo 200 "Photo operation $index"
        $result = $photo.Content | ConvertFrom-Json
        $food = $result.candidates[0].content.parts[0].text | ConvertFrom-Json
        if (-not $food.is_food -or $food.detected_foods.Count -lt 1) { throw 'Vision food contract failed.' }
        Write-Output "Photo operation $index returned complete food JSON ($($food.detected_foods.Count) items)."
    }
    if ($NonFoodImage) {
        $nonFood = [Convert]::ToBase64String([IO.File]::ReadAllBytes($NonFoodImage))
        foreach ($index in @(0, 1)) {
            $prompt = $prompts[$index].Groups[1].Value.Replace('$validationThreshold', '0.65')
            $rejected = Send-Request 'generate' @{
                contents = @(@{ parts = @(@{ text = $prompt }, @{ inlineData = @{ mimeType = 'image/jpeg'; data = $nonFood } }) })
                generationConfig = @{ responseMimeType = 'application/json'; maxOutputTokens = 4096 }
            } $session
            Assert-Status $rejected 200 "Non-food operation $index"
            $rejection = ($rejected.Content | ConvertFrom-Json).candidates[0].content.parts[0].text | ConvertFrom-Json
            if ($rejection.is_food -ne $false -or $rejection.detected_foods.Count -ne 0) { throw 'Non-food guardrail failed.' }
            Write-Output "Non-food operation $index correctly rejected the image."
        }
    }
    $insightPrompt = $prompts[2].Groups[1].Value.Replace('$foodsSummary', 'Rice, dal, aloo sabzi, salad').Replace('${profile.aiContext()}', '')
    $chatPrompt = [regex]::Match($source, '(?s)val systemContext = """\r?\n(.*?)\r?\n"""\.trimIndent\(\)').Groups[1].Value
    $chatPrompt = $chatPrompt.Replace('${profile.aiContext()}', '')
    $replacements = @{
        '${total.calories}' = '518'; '${total.protein}' = '16.1'; '${total.carbs}' = '94.9'; '${total.fat}' = '8.6'
        '$foodListDesc' = 'Rice 180g, dal 150g, aloo sabzi 120g, salad 50g'; '$addedDesc' = 'No added foods'
        '$removedDesc' = 'No removed foods'; '$question' = 'How could I add more protein?'
    }
    foreach ($key in $replacements.Keys) {
        $insightPrompt = $insightPrompt.Replace($key, $replacements[$key])
        $chatPrompt = $chatPrompt.Replace($key, $replacements[$key])
    }
    $insights = Send-Request 'generate' @{
        contents = @(@{ parts = @(@{ text = $insightPrompt }) })
        generationConfig = @{ responseMimeType = 'application/json' }
    } $session
    Assert-Status $insights 200 'Nutrition insights'
    $insightResult = $insights.Content | ConvertFrom-Json
    $insightData = $insightResult.candidates[0].content.parts[0].text | ConvertFrom-Json
    if (-not $insightData.headline -or -not $insightData.suggestions) { throw 'Insights contract failed.' }
    $chat = Send-Request 'generate' @{ contents = @(@{ parts = @(@{ text = $chatPrompt }) }) } $session
    Assert-Status $chat 200 'Meal chat'
    $chatResult = $chat.Content | ConvertFrom-Json
    if (-not $chatResult.candidates[0].content.parts[0].text) { throw 'Chat contract failed.' }
}
$session = $null
$identityToken = $null
Write-Output "Smoke tests passed for $serverUrl"
