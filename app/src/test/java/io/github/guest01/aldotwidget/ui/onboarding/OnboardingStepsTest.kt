package io.github.guest01.aldotwidget.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingStepsTest {

    @Test
    fun `처음 설치하면 세 단계를 순서대로`() {
        assertEquals(
            listOf(OnboardingStep.NOTIFICATIONS, OnboardingStep.BATTERY, OnboardingStep.WIDGET),
            onboardingSteps(needsNotifications = true, needsBattery = true, needsWidget = true),
        )
    }

    @Test
    fun `이미 끝난 단계는 건너뛴다`() {
        assertEquals(
            listOf(OnboardingStep.BATTERY),
            onboardingSteps(needsNotifications = false, needsBattery = true, needsWidget = false),
        )
    }

    @Test
    fun `모두 끝났으면 안내 없이 바로 끝난다`() {
        assertEquals(
            emptyList<OnboardingStep>(),
            onboardingSteps(needsNotifications = false, needsBattery = false, needsWidget = false),
        )
    }
}
