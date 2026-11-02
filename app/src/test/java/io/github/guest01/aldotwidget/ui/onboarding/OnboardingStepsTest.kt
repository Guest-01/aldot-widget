package io.github.guest01.aldotwidget.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingStepsTest {

    @Test
    fun `처음 설치하면 세 단계를 순서대로`() {
        assertEquals(
            listOf(OnboardingStep.NOTIFICATIONS, OnboardingStep.BATTERY, OnboardingStep.WIDGET),
            onboardingSteps(needsNotifications = true, needsBattery = true),
        )
    }

    @Test
    fun `이미 허용된 단계는 건너뛴다`() {
        assertEquals(
            listOf(OnboardingStep.BATTERY, OnboardingStep.WIDGET),
            onboardingSteps(needsNotifications = false, needsBattery = true),
        )
    }

    @Test
    fun `위젯 단계는 테마 색을 고르는 곳이라 항상 보여 준다`() {
        assertEquals(
            listOf(OnboardingStep.WIDGET),
            onboardingSteps(needsNotifications = false, needsBattery = false),
        )
    }
}
