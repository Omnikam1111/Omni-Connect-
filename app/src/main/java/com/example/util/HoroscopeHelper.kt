package com.example.util

import java.util.Calendar

data class HoroscopeInfo(
    val name: String,
    val dates: String,
    val elementModality: String,
    val ruler: String,
    val coreDrive: String,
    val deepestFear: String,
    val loveLanguage: String,
    val moneyStyle: String,
    val shadow: String,
    val sexualSignature: String
)

object HoroscopeHelper {
    fun getHoroscope(birthdayInMillis: Long?): HoroscopeInfo? {
        if (birthdayInMillis == null) return null
        val cal = Calendar.getInstance().apply {
            timeInMillis = birthdayInMillis
        }
        val month = cal.get(Calendar.MONTH) // 0-indexed, Jan is 0, Dec is 11
        val day = cal.get(Calendar.DAY_OF_MONTH)

        return when (month) {
            Calendar.JANUARY -> if (day < 20) Capricorn else Aquarius
            Calendar.FEBRUARY -> if (day < 19) Aquarius else Pisces
            Calendar.MARCH -> if (day < 21) Pisces else Aries
            Calendar.APRIL -> if (day < 20) Aries else Taurus
            Calendar.MAY -> if (day < 21) Taurus else Gemini
            Calendar.JUNE -> if (day < 21) Gemini else Cancer
            Calendar.JULY -> if (day < 23) Cancer else Leo
            Calendar.AUGUST -> if (day < 23) Leo else Virgo
            Calendar.SEPTEMBER -> if (day < 23) Virgo else Libra
            Calendar.OCTOBER -> if (day < 23) Libra else Scorpio
            Calendar.NOVEMBER -> if (day < 22) Scorpio else Sagittarius
            Calendar.DECEMBER -> if (day < 22) Sagittarius else Capricorn
            else -> Capricorn
        }
    }

    private val Aries = HoroscopeInfo(
        name = "ARIES",
        dates = "March 21 – April 19",
        elementModality = "Fire / Cardinal (initiating)",
        ruler = "Mars (aggression, desire)",
        coreDrive = "To be first, to conquer, to act now.",
        deepestFear = "Boredom, being controlled, losing autonomy.",
        loveLanguage = "Physical touch, words of affirmation (but only spontaneous ones).",
        moneyStyle = "Impulsive earner, faster spender. Loves startups, competition-based income. Hates budgets.",
        shadow = "Selfishness, rage when blocked, inability to finish what they start.",
        sexualSignature = "Explosive, direct, adventurous. Needs a chase."
    )

    private val Taurus = HoroscopeInfo(
        name = "TAURUS",
        dates = "April 20 – May 20",
        elementModality = "Earth / Fixed (stubborn, stable)",
        ruler = "Venus (beauty, pleasure, but in earthy mode)",
        coreDrive = "To accumulate, to savor, to feel safe.",
        deepestFear = "Sudden change, poverty, betrayal of routine.",
        loveLanguage = "Acts of service, physical touch (sensual, not just sexual).",
        moneyStyle = "Earns steadily, invests in tangible assets. Hates debt. Great at saving but can be miserly.",
        shadow = "Possessiveness, laziness, refusal to adapt.",
        sexualSignature = "Slow, luxurious, deeply sensory. Needs trust and a beautiful environment."
    )

    private val Gemini = HoroscopeInfo(
        name = "GEMINI",
        dates = "May 21 – June 20",
        elementModality = "Air / Mutable (adaptable, scattered)",
        ruler = "Mercury (communication, trickster)",
        coreDrive = "To gather information, to connect, to keep options open.",
        deepestFear = "Being trapped, boring others, missing out.",
        loveLanguage = "Words of affirmation (intellectual flirting), quality time (but varied).",
        moneyStyle = "Multiple income streams. Great at side hustles. Loses interest in long-term investments.",
        shadow = "Superficiality, emotional unavailability, gossip, lying by omission.",
        sexualSignature = "Playful, inventive, but needs novelty. Easily distracted."
    )

    private val Cancer = HoroscopeInfo(
        name = "CANCER",
        dates = "June 21 – July 22",
        elementModality = "Water / Cardinal (initiating emotion)",
        ruler = "Moon (moods, mothering, the past)",
        coreDrive = "To nurture, to belong, to feel emotionally secure.",
        deepestFear = "Abandonment, emotional exposure, homelessness of the heart.",
        loveLanguage = "Acts of service, quality time, physical touch (clinging).",
        moneyStyle = "Saves for home, family, food security. Emotional spending when upset. Good at real estate.",
        shadow = "Manipulative guilt-tripping, mood swings, passive aggression, smothering.",
        sexualSignature = "Deeply emotional, needs aftercare. Sex = emotional bonding."
    )

    private val Leo = HoroscopeInfo(
        name = "LEO",
        dates = "July 23 – August 22",
        elementModality = "Fire / Fixed (steady ego)",
        ruler = "Sun (core self, creativity)",
        coreDrive = "To shine, to be admired, to create joy.",
        deepestFear = "Being ignored, ridicule, not being special.",
        loveLanguage = "Words of affirmation (praise), gifts (luxurious), physical touch (dramatic).",
        moneyStyle = "Generous to a fault. Loves luxury, brand names. Earns through performance, leadership, or risk-taking.",
        shadow = "Arrogance, drama addiction, jealousy, need for constant applause.",
        sexualSignature = "Passionate, performative but sincere. Needs to feel like a king/queen."
    )

    private val Virgo = HoroscopeInfo(
        name = "VIRGO",
        dates = "August 23 – September 22",
        elementModality = "Earth / Mutable (analytical, adaptable)",
        ruler = "Mercury (but in detail-oriented mode)",
        coreDrive = "To perfect, to serve, to analyze.",
        deepestFear = "Chaos, criticism (ironic, as they are critical), being useless.",
        loveLanguage = "Acts of service, words of affirmation (practical advice as love).",
        moneyStyle = "Meticulous budgeter. Spreadsheets, coupons, investments in health and tools. Worries about money even when rich.",
        shadow = "Hypochondria, nitpicking, martyr complex, coldness when disappointed.",
        sexualSignature = "Service-oriented, technical. Needs cleanliness and a mental connection."
    )

    private val Libra = HoroscopeInfo(
        name = "LIBRA",
        dates = "September 23 – October 22",
        elementModality = "Air / Cardinal (initiating harmony)",
        ruler = "Venus (beauty, justice, relationships)",
        coreDrive = "To create peace, beauty, and fair partnerships.",
        deepestFear = "Conflict, ugliness, being alone, making wrong decisions.",
        loveLanguage = "Gift-giving (aesthetic), quality time (romantic settings), words of affirmation.",
        moneyStyle = "Spends on art, fashion, socializing. Can be indecisive about investments. Often marries wealth or attracts financial partners.",
        shadow = "People-pleasing, passive aggression, indecisiveness, superficiality, flirting to avoid commitment.",
        sexualSignature = "Romantic, graceful, but sometimes performative. Needs beauty and equality."
    )

    private val Scorpio = HoroscopeInfo(
        name = "SCORPIO",
        dates = "October 23 – November 21",
        elementModality = "Water / Fixed (intense, unyielding)",
        ruler = "Pluto (transformation) + traditional Mars (war)",
        coreDrive = "To penetrate truth, to merge, to control the uncontrollable.",
        deepestFear = "Betrayal, vulnerability, being powerless.",
        loveLanguage = "Physical touch (deep, not casual), quality time (undivided attention), acts of service (loyalty tests).",
        moneyStyle = "Intense earner, often through other people's resources (investments, inheritance, psychology). Secretive about wealth. Uses money as power.",
        shadow = "Jealousy, vindictiveness, emotional terrorism, secrecy, obsession.",
        sexualSignature = "Transformative, kink-friendly, all-or-nothing. Sex as spiritual death/rebirth."
    )

    private val Sagittarius = HoroscopeInfo(
        name = "SAGITTARIUS",
        dates = "November 22 – December 21",
        elementModality = "Fire / Mutable (expansive, restless)",
        ruler = "Jupiter (luck, philosophy, travel)",
        coreDrive = "To explore truth, to expand horizons, to find meaning.",
        deepestFear = "Trapped by routine, ignorance, being tied down.",
        loveLanguage = "Quality time (adventures), words of affirmation (big ideas, humor).",
        moneyStyle = "Lucky earner. Spends on travel, education, gambling. Hates budgets but is generous. Often recovers from losses.",
        shadow = "Recklessness, brutal honesty, commitment phobia, hypocrisy (preaches freedom but wants own way).",
        sexualSignature = "Adventurous, humorous, athletic. Needs philosophical connection or it's boring."
    )

    private val Capricorn = HoroscopeInfo(
        name = "CAPRICORN",
        dates = "December 22 – January 19",
        elementModality = "Earth / Cardinal (ambitious, initiating structure)",
        ruler = "Saturn (discipline, time, limits)",
        coreDrive = "To achieve, to build legacy, to control outcomes.",
        deepestFear = "Failure, poverty, looking foolish, losing status.",
        loveLanguage = "Acts of service, gifts (practical, high-quality), quality time (scheduled).",
        moneyStyle = "Master planner. Long-term investments, delayed gratification. Earns through authority, real estate, tradition. Frugal but not cheap.",
        shadow = "Workaholism, emotional repression, pessimism, using people as ladder rungs.",
        sexualSignature = "Disciplined, traditional or kinky as release. Needs respect more than passion."
    )

    private val Aquarius = HoroscopeInfo(
        name = "AQUARIUS",
        dates = "January 20 – February 18",
        elementModality = "Air / Fixed (stable rebellion)",
        ruler = "Uranus (innovation, shock) + Saturn (old ruler)",
        coreDrive = "To liberate, to innovate, to challenge norms.",
        deepestFear = "Conformity, emotional engulfment, being ordinary.",
        loveLanguage = "Quality time (intellectual exchange), words of affirmation (original compliments). Hates traditional romance.",
        moneyStyle = "Erratic but brilliant. Tech, group projects, social causes. Spends on gadgets, friends. Detached from money's emotional weight.",
        shadow = "Detachment, coldness, unpredictability, superiority complex, ghosting.",
        sexualSignature = "Experimental, friendly but impersonal. Needs mental arousal first. Can disconnect during intimacy."
    )

    private val Pisces = HoroscopeInfo(
        name = "PISCES",
        dates = "February 19 – March 20",
        elementModality = "Water / Mutable (fluid, dissolving)",
        ruler = "Neptune (dreams, illusion, transcendence) + Jupiter (traditional)",
        coreDrive = "To transcend ego, to feel unity, to heal.",
        deepestFear = "Reality's harshness, being trapped in the mundane, losing their dream.",
        loveLanguage = "Physical touch (ethereal), words of affirmation (poetic), quality time (merging).",
        moneyStyle = "No sense of money. Spends on art, drugs, charity. Attracts scams and saviors. Needs a financial manager.",
        shadow = "Escapism, victimhood, addiction, boundarylessness, lies to avoid hurting.",
        sexualSignature = "Fluid, spiritual, sometimes sacrificial. Sex as merging into oneness. Can be passive"
    )
}
