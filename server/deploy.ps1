$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true
$project = 'gen-lang-client-0100408368'
$region = 'us-central1'
$serviceAccount = "kalc-vertex@$project.iam.gserviceaccount.com"
$secretName = 'kalc-server-auth'

Push-Location $PSScriptRoot
try {
    node --test
    node create-secrets.mjs
    $configPath = Join-Path $env:LOCALAPPDATA 'KalC/server-auth.json'
    gcloud services enable secretmanager.googleapis.com --project=$project --quiet
    $accounts = gcloud iam service-accounts list --project=$project --format='value(email)'
    if ($serviceAccount -notin $accounts) {
        gcloud iam service-accounts create kalc-vertex --display-name='KalC Vertex runtime' --project=$project --quiet
    }
    gcloud projects add-iam-policy-binding $project --member="serviceAccount:$serviceAccount" --role=roles/aiplatform.user --condition=None --quiet --format='value(etag)'
    $secrets = gcloud secrets list --project=$project --format='value(name)'
    if ($secretName -notin $secrets) {
        gcloud secrets create $secretName --replication-policy=automatic --data-file=$configPath --project=$project --quiet
    } else {
        gcloud secrets versions add $secretName --data-file=$configPath --project=$project --quiet
    }
    gcloud secrets add-iam-policy-binding $secretName --member="serviceAccount:$serviceAccount" --role=roles/secretmanager.secretAccessor --condition=None --project=$project --quiet --format='value(etag)'
    gcloud run deploy kalc-vertex --source=. --project=$project --region=$region --service-account=$serviceAccount --no-allow-unauthenticated --min=0 --max=1 --concurrency=8 --memory=256Mi --cpu=1 --timeout=75 --set-secrets="AUTH_CONFIG=${secretName}:latest" --set-env-vars="GOOGLE_CLOUD_PROJECT=$project,VERTEX_MODEL=gemini-3.1-flash-lite" --quiet
    Write-Output 'Deployed privately. Verify wrong-passkey rejection and an authorized Vertex call before enabling app access.'
} finally {
    Pop-Location
}
