package com.example

import com.example.feature.about.CoffeeOption
import com.example.feature.about.SponsorTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutScreenTest {

    @Test
    fun coffeeOptionData_createsValidPriceAndEmoji() {
        val option = CoffeeOption("☕", "1.5$", "coffee_1")
        assertEquals("☕", option.emoji)
        assertEquals("1.5$", option.price)
        assertEquals("coffee_1", option.id)
    }

    @Test
    fun sponsorTierData_identifiesPopularTier() {
        val popularTier = SponsorTier("pelindung", "Pelindung", "⭐", "9.99$ / bulan", isPopular = true)
        val normalTier = SponsorTier("teman", "Teman", "🌱", "1.99$ / bulan")

        assertTrue(popularTier.isPopular)
        assertEquals("Pelindung", popularTier.name)
        assertEquals("9.99$ / bulan", popularTier.priceMonthly)
        assertEquals(false, normalTier.isPopular)
    }
}
