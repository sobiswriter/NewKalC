package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.auth.AiProvider
import com.example.data.auth.VertexAccessRepository
import com.example.data.gemini.GeminiFoodService
import com.example.data.profile.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonalizationLiveTest {
    @Test fun syntheticOptInProfileWorksWithLiveVertex() = runBlocking {
        val passkey = InstrumentationRegistry.getArguments().getString("serverPasskey")
        assumeTrue("Live verification requires the owner's passkey at runtime.", !passkey.isNullOrBlank())
        val connection = VertexAccessRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        val previous = connection.provider
        try {
            connection.selectProvider(AiProvider.VERTEX)
            connection.unlock(passkey!!).getOrThrow()
            val service = GeminiFoodService(connection)
            val meal = service.getDeterministicFallbackAnalysis("rice dal aloo sabzi salad")
            val synthetic = UserProfile(age = 29, heightCm = 170.0, weightKg = 68.0,
                goal = "Add more plant protein", preferences = "Vegetarian", personalizationEnabled = true)
            val insights = service.generateNutritionInsights(meal, profile = synthetic).getOrThrow()
            assertTrue(insights.headline.isNotBlank() && insights.suggestions.isNotEmpty())
            val answer = service.askAboutFood("Suggest one vegetarian protein addition that fits my goal.",
                meal, emptyList(), null, profile = synthetic).getOrThrow()
            assertTrue(answer.isNotBlank())
        } finally {
            connection.lock()
            connection.selectProvider(previous)
        }
    }
}
