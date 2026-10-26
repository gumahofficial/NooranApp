package com.gumah.nooran

import androidx.test.core.app.ApplicationProvider
import com.gumah.nooran.model.Post
import com.gumah.nooran.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppContextLoads() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertNotNull(context)
        assertEquals("com.gumah.nooran", context.packageName)
    }

    @Test
    fun testPostModelCreation() {
        val post = Post(
            id = "test_post_1",
            userId = "user_123",
            authorName = "أحمد",
            authorUsername = "ahmed",
            content = "كلمة طيبة كشجرة طيبة",
            impactScore = 10L
        )
        assertEquals("ahmed", post.authorUsername)
        assertEquals(10L, post.impactScore)
    }

    @Test
    fun testUserProfileDefaults() {
        val profile = UserProfile(
            uid = "u1",
            displayName = "نور",
            username = "noor"
        )
        assertEquals("✨", profile.avatarSymbol)
        assertEquals(false, profile.isVerified)
    }

    @Test
    fun testAuthValidators() {
        // Email validation
        val validEmailErr = com.gumah.nooran.util.AuthValidators.validateEmail("user@example.com")
        assertEquals(null, validEmailErr)

        val invalidEmailErr = com.gumah.nooran.util.AuthValidators.validateEmail("invalid-email")
        assertNotNull(invalidEmailErr)

        // Username validation
        val validUser = com.gumah.nooran.util.AuthValidators.validateUsername("nooran_dev")
        assertEquals(com.gumah.nooran.util.AuthValidators.UsernameValidationResult.Valid, validUser)

        val reservedUser = com.gumah.nooran.util.AuthValidators.validateUsername("admin")
        assert(reservedUser is com.gumah.nooran.util.AuthValidators.UsernameValidationResult.Invalid)

        // Suggestions
        val suggestions = com.gumah.nooran.util.AuthValidators.generateUsernameSuggestions("ahmed")
        assert(suggestions.isNotEmpty())

        // Password strength
        val strongPass = com.gumah.nooran.util.AuthValidators.evaluatePasswordStrength("Nooran2026!Secret")
        assertEquals(com.gumah.nooran.util.AuthValidators.PasswordStrength.STRONG, strongPass)
    }
}
