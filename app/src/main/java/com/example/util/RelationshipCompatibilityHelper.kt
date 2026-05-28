package com.example.util

data class CompatibilityInfo(
    val signA: String,
    val signB: String,
    val rating: Int,
    // For Split Details (1-25)
    val love: String? = null,
    val communication: String? = null,
    val finances: String? = null,
    val intimacy: String? = null,
    val conflict: String? = null,
    val parenting: String? = null,
    val longTermPotentialString: String? = null,
    val shadowWarning: String? = null,
    // For Condensed Details (26-78)
    val condensedText: String? = null
) {
    val ratingLegendString: String
        get() = when (rating) {
            in 85..100 -> "Natural harmony. Little work needed."
            in 65..84 -> "Good match with some friction."
            in 40..64 -> "Workable with maturity; frequent misunderstandings."
            else -> "High conflict; only for evolved individuals."
        }
}

object RelationshipCompatibilityHelper {

    fun getCompatibility(sign1: String, sign2: String): CompatibilityInfo {
        val s1 = sign1.trim().uppercase()
        val s2 = sign2.trim().uppercase()
        val key = if (s1 <= s2) "${s1}_${s2}" else "${s2}_$s1"
        return compatibilityMap[key] ?: CompatibilityInfo(
            signA = s1,
            signB = s2,
            rating = 50,
            condensedText = "A match between $s1 and $s2 holds potential. Communication is key, as is understanding each other's elemental modality."
        )
    }

    private val compatibilityMap = mapOf(
        // 1. ARIES + ARIES
        "ARIES_ARIES" to CompatibilityInfo(
            signA = "ARIES", signB = "ARIES", rating = 30,
            love = "Explosive start, instant attraction. Both want to lead. Passionate fights and make-ups. Long-term: exhaust each other. No one yields.",
            communication = "Direct, sometimes yelling. No passive aggression but harsh words.",
            finances = "Disaster. Both impulsive spenders. Competitive earning – might start businesses together then fight over control.",
            intimacy = "Energetic, frequent, but lacks tenderness. Power struggles in bed.",
            conflict = "Every argument is a war. Neither apologizes first. Can burn out quickly.",
            parenting = "Fun but inconsistent. Children feel chaos.",
            longTermPotentialString = "30% – great for short-term fling or open marriage, but monogamous marriage is a bonfire.",
            shadowWarning = "Two unchecked Mars energies = physical violence risk if immature."
        ),
        // 2. ARIES + LEO
        "ARIES_LEO" to CompatibilityInfo(
            signA = "ARIES", signB = "LEO", rating = 80,
            love = "Royal fire. Leo admires Aries’ courage; Aries worships Leo’s confidence. Great passion, lots of laughter.",
            communication = "Warm, but Aries’ bluntness can wound Leo’s ego. Leo’s need for praise may exhaust Aries.",
            finances = "Leo wants luxury; Aries wants spontaneity. Both earn well if motivated, but overspending on “impressing each other” is a trap.",
            intimacy = "High voltage. Leo performs, Aries conquers. Both satisfied but can become competitive.",
            conflict = "Leo sulks; Aries explodes. Leo needs repair rituals; Aries forgets quickly. Works if Aries gives compliments.",
            parenting = "Enthusiastic, dramatic. Children feel adored but may lack structure.",
            longTermPotentialString = "80% – one of the best fire-fire pairs. Both fixed and cardinal balance each other’s initiation vs. stability.",
            shadowWarning = "Ego clashes. Both must learn to share the spotlight."
        ),
        // 3. ARIES + SAGITTARIUS
        "ARIES_SAGITTARIUS" to CompatibilityInfo(
            signA = "ARIES", signB = "SAGITTARIUS", rating = 75,
            love = "Adventurous, free-spirited. Both hate boredom. Travel, sports, philosophical debates. No jealousy.",
            communication = "Honest to a fault. Sagittarius’ “truth” can be brutal; Aries’ impatience cuts conversations short. But they laugh it off.",
            finances = "Reckless. Both gamble, invest in risky startups, forget bills. Sag’s optimism + Aries’ impulsivity = boom or bankruptcy. Need an earth sign accountant.",
            intimacy = "Fun, athletic, experimental. No emotional depth required. Can feel hollow over time.",
            conflict = "Neither holds grudges. Fights explode, then over in minutes. No emotional baggage – but also no deep resolution.",
            parenting = "Cool parents. Encourage independence. May neglect routine.",
            longTermPotentialString = "75% – very high for non-traditional relationships (open, nomadic). Low for domestic stability.",
            shadowWarning = "Commitment-phobia squared. Both may stray not from malice but from restlessness."
        ),
        // 4. LEO + LEO
        "LEO_LEO" to CompatibilityInfo(
            signA = "LEO", signB = "LEO", rating = 55,
            love = "Two suns in one sky. Initially magnetic, each sees the other as royalty. Eventually, who shines brighter?",
            communication = "Dramatic, loud, affectionate. Lots of “I love you”s but also silent treatments when egos clash.",
            finances = "Competitive spending. Both buy luxury to impress. Can be wealthy if they channel rivalry into joint business. Usually need separate accounts.",
            intimacy = "Theatrical and passionate. Great sex but needs applause. Can become a performance rather than connection.",
            conflict = "Cold wars. Neither yields. Both wait for the other to bow. Can last weeks. Third party mediation needed.",
            parenting = "Over-the-top. Children become little princes/princesses. Risk of spoiling.",
            longTermPotentialString = "55% – works only if they have different arenas (e.g., one is star at work, the other at home). Same field = disaster.",
            shadowWarning = "Narcissistic injury leads to revenge cheating."
        ),
        // 5. LEO + SAGITTARIUS
        "LEO_SAGITTARIUS" to CompatibilityInfo(
            signA = "LEO", signB = "SAGITTARIUS", rating = 85,
            love = "Bonfire. Leo’s loyalty + Sag’s adventure = life as a festival. Both optimistic. Leo wants to be worshipped; Sag worships freedom but will worship Leo for a while.",
            communication = "Jovial, philosophical, flirtatious. Sag can be too blunt for Leo’s pride, but Leo’s forgiveness is quick if Sag is charming.",
            finances = "Leo saves for status; Sag spends on experience. Middle ground: invest in travel or entertainment business. Good earning synergy.",
            intimacy = "Enthusiastic, playful. Sag likes variety; Leo likes adoration. Works if Leo initiates and Sag follows – sometimes.",
            conflict = "Sag disappears (needs space); Leo demands attention. Sag feels trapped; Leo feels abandoned. Compromise: scheduled adventure nights.",
            parenting = "Fun, inspiring, but inconsistent. Children get mixed messages on discipline.",
            longTermPotentialString = "85% – one of the strongest fire pairings. Sag’s mutability adapts to Leo’s fixity; Leo’s warmth keeps Sag returning.",
            shadowWarning = "Sag’s flirting triggers Leo’s jealousy; Leo’s possessiveness triggers Sag’s flight."
        ),
        // 6. SAGITTARIUS + SAGITTARIUS
        "SAGITTARIUS_SAGITTARIUS" to CompatibilityInfo(
            signA = "SAGITTARIUS", signB = "SAGITTARIUS", rating = 40,
            love = "Two gypsies. No jealousy, no schedule, no rules. Beautiful for 1–3 years. Then: who holds the anchor?",
            communication = "Endless talk about ideas, rarely about feelings. Both avoid vulnerability. Great debates, weak heart-to-heart.",
            finances = "Yikes. Double gambling, double last-minute trips, double unpaid credit cards. Only works if one has strong Capricorn placements.",
            intimacy = "Fun, adventurous, but emotionally detached. Neither asks “how was that for you?” Both move on quickly.",
            conflict = "They don’t fight – they leave. Commitment issues cause silent drifting. No closure.",
            parenting = "Kids become global nomads but lack stability. May struggle with school routine.",
            longTermPotentialString = "40% – amazing as friends-with-benefits or open marriage. Monogamous marriage feels like prison to both.",
            shadowWarning = "Mutual avoidance of responsibility. House never gets repaired. Bills pile up."
        ),
        // 7. TAURUS + TAURUS
        "TAURUS_TAURUS" to CompatibilityInfo(
            signA = "TAURUS", signB = "TAURUS", rating = 95,
            love = "Two oaks in a forest. Extremely loyal, sensual, peaceful. Slow to start, nearly impossible to break.",
            communication = "Few words, but understood. Silence is comfortable. Stubborn debates – neither budges. Need a mediator.",
            finances = "Excellent. Both save, invest in real estate, hate debt. Build generational wealth. Only risk: too conservative, miss opportunities.",
            intimacy = "Deeply sensual, routine-based. Same lovemaking script for decades – comforting to them, boring to others.",
            conflict = "Passive resistance. Taurus doesn’t explode; they shut down and wait. Can last weeks. No resolution, just forgetting.",
            parenting = "Stable, overprotective, materialistic. Children get everything but may feel stifled.",
            longTermPotentialString = "95% – divorce almost unheard of. But risk: loveless comfort, staying together out of habit.",
            shadowWarning = "Mutual possessiveness. “You are mine” can become suffocating."
        ),
        // 8. TAURUS + VIRGO
        "TAURUS_VIRGO" to CompatibilityInfo(
            signA = "TAURUS", signB = "VIRGO", rating = 90,
            love = "Earthy paradise. Virgo’s service makes Taurus feel cared for; Taurus’ stability soothes Virgo’s anxiety. Slow, deep love.",
            communication = "Virgo talks details; Taurus listens patiently. Virgo appreciates Taurus’ practicality. Rare misunderstandings.",
            finances = "Top tier. Virgo budgets; Taurus invests. Home renovation, gardening, joint accounts. No debt.",
            intimacy = "Virgo aims to please; Taurus enjoys. Taurus teaches Virgo to relax. Virgo’s occasional criticism hurts Taurus, but Virgo learns.",
            conflict = "Virgo nags; Taurus resists. Virgo overanalyzes; Taurus shuts down. Resolution: physical affection first, then talk.",
            parenting = "Consistent, health-conscious, a bit rigid. Children learn responsibility but may lack spontaneity.",
            longTermPotentialString = "90% – one of the most stable pairings. Only risk: boredom. Need a shared creative project.",
            shadowWarning = "Virgo’s perfectionism can make Taurus feel “not good enough.” Taurus’ stubbornness can make Virgo feel unheard."
        ),
        // 9. TAURUS + CAPRICORN
        "TAURUS_CAPRICORN" to CompatibilityInfo(
            signA = "TAURUS", signB = "CAPRICORN", rating = 85,
            love = "Power earth. Both value loyalty, tradition, and material security. Taurus brings pleasure; Capricorn brings ambition. A corporate romance.",
            communication = "Direct, respectful, economical. No drama. Both hate small talk. Capricorn’s dry humor delights Taurus.",
            finances = "Monstrous wealth potential. Capricorn strategizes; Taurus executes slowly and steadily. Real estate empire.",
            intimacy = "Taurus wants sensual slow; Capricorn often too tired or goal-focused. Cap needs to schedule intimacy – Taurus may feel unloved.",
            conflict = "Both repress emotions. Capricorn withdraws into work; Taurus withdraws into food/TV. Can become roommates. Crisis: who cracks first?",
            parenting = "Authoritative but fair. High expectations, strong boundaries. Children succeed materially but may lack emotional warmth.",
            longTermPotentialString = "85% – highly durable. Divorce rate low. But emotional desert possible.",
            shadowWarning = "Workaholism + comfort addiction = no emotional growth for decades."
        ),
        // 10. VIRGO + VIRGO
        "VIRGO_VIRGO" to CompatibilityInfo(
            signA = "VIRGO", signB = "VIRGO", rating = 70,
            love = "Two perfectionists in a clean house. Mutual understanding of neuroses. Tender, but can become a critique fest.",
            communication = "Detailed, analytical. They finish each other’s sentences about chores. Rarely talk about feelings – feelings are “inefficient.”",
            finances = "Extremely organized. Spreadsheets for everything. Savings, coupons, investments. May be too frugal to enjoy life.",
            intimacy = "Clinical or service-oriented. Need to schedule “sexy time.” Lack of spontaneity can kill passion. Both worry about germs.",
            conflict = "Passive-aggressive note-leaving. Silent treatment. Both wait for the other to admit fault first. Can last forever.",
            parenting = "Over-scheduled, healthy, but anxious. Children feel pressure to be perfect.",
            longTermPotentialString = "70% – works if they have separate hobbies. Same job = nightmare. Good for companionship marriage.",
            shadowWarning = "Mutual nitpicking erodes self-esteem. “You loaded the dishwasher wrong” becomes a war."
        ),
        // 11. VIRGO + CAPRICORN
        "VIRGO_CAPRICORN" to CompatibilityInfo(
            signA = "VIRGO", signB = "CAPRICORN", rating = 90,
            love = "Earth power couple. Virgo details, Capricorn vision. Respectful, loyal, understated. Slow-burn romance that lasts decades.",
            communication = "Efficient, professional. Both appreciate competence. Capricorn’s ambition inspires Virgo’s work ethic. Very low drama.",
            finances = "Excellent. Virgo manages day-to-day; Capricorn plans 20-year goals. No impulsive spending. Early retirement.",
            intimacy = "Capricorn wants respect; Virgo wants to be needed. Can become transactional (“if you do this chore, we have sex”). But deep devotion exists beneath.",
            conflict = "Capricorn dismisses Virgo’s worries as “small”; Virgo resents Capricorn’s emotional coldness. Both need to learn emotional expression.",
            parenting = "Structured, disciplined, success-oriented. Children are high achievers but may feel love is conditional on performance.",
            longTermPotentialString = "90% – exceptional for building a life. Only missing: spontaneity and emotional rawness.",
            shadowWarning = "Both can become workaholics who forget they are lovers."
        ),
        // 12. CAPRICORN + CAPRICORN
        "CAPRICORN_CAPRICORN" to CompatibilityInfo(
            signA = "CAPRICORN", signB = "CAPRICORN", rating = 75,
            love = "Two CEOs at the dinner table. Mutual respect, shared ambition. Romance is subtle: loyalty, provision, legacy. No flowers, but will pay for your mother’s surgery.",
            communication = "Brief, factual, future-oriented. Neither whines. Emotional topics are avoided. Can feel like a board meeting.",
            finances = "Stunning. Double income, no debt, multiple properties. They die rich. Only risk: never enjoying money.",
            intimacy = "Duty-driven or scheduled. Capricorns often have a “sex on Saturday at 9pm” agreement. Works for them, but passion may fade into routine.",
            conflict = "Stone cold silence. Each waits for the other to bend. Neither does. Can separate without a word. Reconciliation is rare because both see it as “failure.”",
            parenting = "Strict, traditional, high-pressure. Children may rebel or become overachievers.",
            longTermPotentialString = "75% – stable but lonely. Great for business partners, challenging for romantic warmth.",
            shadowWarning = "Emotional starvation. Both need a water sign friend to remind them to feel."
        ),
        // 13. GEMINI + GEMINI
        "GEMINI_GEMINI" to CompatibilityInfo(
            signA = "GEMINI", signB = "GEMINI", rating = 35,
            love = "Twin flames in chaos. Endless conversation, witty banter, constant new experiences. But who is home for dinner?",
            communication = "The best of all pairs – verbal acrobatics. But they talk over each other. Feelings are analyzed, not felt.",
            finances = "Erratic. Multiple side hustles, but forget bills. Both spend on gadgets, books, classes. Need an earth sign accountant.",
            intimacy = "Playful, inventive, but emotionally shallow. Sex as a game. Both get bored quickly; may seek novelty elsewhere.",
            conflict = "No lasting fights – they just get distracted. But also no deep resolution. Issues are “talked to death” then ignored.",
            parenting = "Fun, intellectual, inconsistent. Children are stimulated but lack routine.",
            longTermPotentialString = "35% – works only as open relationship or part-time cohabitation. Traditional marriage feels like death.",
            shadowWarning = "Mutual emotional avoidance. When crisis hits (illness, death), both may flee."
        ),
        // 14. GEMINI + LIBRA
        "GEMINI_LIBRA" to CompatibilityInfo(
            signA = "GEMINI", signB = "LIBRA", rating = 80,
            love = "Airy romance. Libra’s charm + Gemini’s wit = social royalty. Both hate conflict. Lots of dates, art, friends.",
            communication = "Flirtatious, fair, intellectual. Libra mediates when Gemini gets scattered. Gemini keeps Libra from overthinking.",
            finances = "Moderate. Libra spends on beauty; Gemini spends on variety. No natural saver. Good at joint social enterprises (café, gallery).",
            intimacy = "Graceful, playful, but detached. Both prefer talking about sex to having it sometimes. Physical passion can be low.",
            conflict = "Neither wants to fight. They sweep issues under rug until Libra’s passive aggression or Gemini’s sarcasm erupts. Then make up with gifts.",
            parenting = "Polite, cultured, but indecisive. Children learn diplomacy but may lack clear boundaries.",
            longTermPotentialString = "80% – high for companionship marriage. Low for fiery passion. Works if both accept a “best friends who kiss” model.",
            shadowWarning = "Both can avoid hard truths. No one will say “we have a problem.”"
        ),
        // 15. GEMINI + AQUARIUS
        "GEMINI_AQUARIUS" to CompatibilityInfo(
            signA = "GEMINI", signB = "AQUARIUS", rating = 75,
            love = "The intellectual rebellion. Aquarius brings vision; Gemini brings ideas. Both need freedom. No jealousy. Very modern.",
            communication = "Electric. Late-night talks about AI, aliens, politics. But rarely “how do you feel?” Emotional depth is optional.",
            finances = "Unpredictable. Aquarius invests in weird tech; Gemini in many small things. Both hate traditional jobs. Can strike it rich or go broke.",
            intimacy = "Experimental, friendly, sometimes cold. Aquarius may detach mid-act; Gemini may start laughing. Neither takes it too seriously.",
            conflict = "Aquarius withdraws to “think”; Gemini talks in circles. Neither apologizes traditionally. Resolution via intellectual agreement.",
            parenting = "Unconventional. Homeschooling, no gender roles, lots of books. Children are brilliant but may lack emotional security.",
            longTermPotentialString = "75% – excellent for childfree, non-monogamous couples. Traditional family life? No.",
            shadowWarning = "Both can ghost each other when feelings get real."
        ),
        // 16. LIBRA + LIBRA
        "LIBRA_LIBRA" to CompatibilityInfo(
            signA = "LIBRA", signB = "LIBRA", rating = 65,
            love = "A beautiful mirror. Both value romance, fairness, aesthetics. Initial harmony is dreamy. Long-term: who makes decisions?",
            communication = "Polite, diplomatic, but indecisive. “What do you want to eat?” – “I don’t know, what do you want?” – infinite loop.",
            finances = "Spend on beauty, social status, art. Both avoid looking at bills. Can accumulate debt from keeping up appearances. Need a Virgo friend.",
            intimacy = "Graceful, romantic, but performative. Both aim to please, so sex can lack raw authenticity. Over time, can become choreographed.",
            conflict = "They hate it. So they swallow resentment until passive-aggressive jabs. Then makeup sex. Never truly resolve.",
            parenting = "Fair, artistic, but inconsistent discipline. Children learn charm but may manipulate parents.",
            longTermPotentialString = "65% – works if they have separate domains (his closet, her garden). Same career = decision paralysis.",
            shadowWarning = "Mutual people-pleasing to outsiders while neglecting each other’s real needs."
        ),
        // 17. LIBRA + AQUARIUS
        "LIBRA_AQUARIUS" to CompatibilityInfo(
            signA = "LIBRA", signB = "AQUARIUS", rating = 85,
            love = "The visionary + the diplomat. Aquarius shocks; Libra smooths. Great social power couple. Emotionally cool but loyal.",
            communication = "Stimulating. Libra translates Aquarius’ weirdness into charm. Aquarius pushes Libra to think bigger. Respectful debates.",
            finances = "Good. Libra’s taste + Aquarius’ innovation = creative business. Libra manages relationships; Aquarius manages tech.",
            intimacy = "Friendly, unusual (Aquarius), and graceful (Libra). Aquarius may want open relationship; Libra may agree but secretly suffer. Honesty needed.",
            conflict = "Libra wants to talk it out; Aquarius needs alone time. Libra feels rejected; Aquarius feels smothered. Solution: scheduled alone time.",
            parenting = "Progressive, fair, intellectually rich. Children are taught justice and individuality.",
            longTermPotentialString = "85% – one of the strongest air pairings. Emotional depth is the only missing piece – can be found with water sign friends.",
            shadowWarning = "Aquarius’ cold logic can hurt Libra’s need for harmony. Libra’s indecision frustrates Aquarius."
        ),
        // 18. AQUARIUS + AQUARIUS
        "AQUARIUS_AQUARIUS" to CompatibilityInfo(
            signA = "AQUARIUS", signB = "AQUARIUS", rating = 50,
            love = "Two aliens in a pod. Perfect understanding of need for space. No jealousy. But also no traditional romance – birthdays forgotten, anniversaries missed.",
            communication = "Rapid-fire ideas, detached from emotion. They can talk for hours about society, then not speak for days. Both fine with that.",
            finances = "Unpredictable x2. Both donate to weird causes, buy gadgets, forget bills. Wealthy one day, broke the next. Need a Capricorn manager.",
            intimacy = "Experimental, kinky, or non-existent. Both can go months without touch and not mind. Sex is a fun activity, not a need.",
            conflict = "No fighting – just ghosting or logical debate. If emotions arise, both flee. Hard to repair because neither initiates.",
            parenting = "Highly unconventional. Children are free-range, genius-level, but may lack warmth or routine.",
            longTermPotentialString = "50% – amazing as life partners who live separately. Monogamous cohabitation often fails due to boredom.",
            shadowWarning = "Mutual emotional neglect can lead to living as roommates."
        ),
        // 19. CANCER + CANCER
        "CANCER_CANCER" to CompatibilityInfo(
            signA = "CANCER", signB = "CANCER", rating = 80,
            love = "Deep ocean of feeling. Both need security, home, family. Extremely nurturing. Can become codependent.",
            communication = "Emotional telepathy. Few words needed. But mood swings amplify each other. One’s bad day ruins both.",
            finances = "Cautious but emotional. Save for home, children, comfort food. However, both impulse-buy when sad. Joint account drama.",
            intimacy = "Tender, clinging, deeply bonding. Sex as emotional reassurance. Can become suffocating – no personal space.",
            conflict = "Passive aggression, guilt trips, crying. Neither says what’s wrong directly. Can spiral into mutual victimhood. Need a third party.",
            parenting = "Extremely protective, maybe smothering. Children feel loved but may struggle with independence.",
            longTermPotentialString = "80% – very stable if both have outside outlets (career, friends). Without that, emotional drowning.",
            shadowWarning = "Mutual manipulation. “I sacrificed for you” is weaponized."
        ),
        // 20. CANCER + SCORPIO
        "CANCER_SCORPIO" to CompatibilityInfo(
            signA = "CANCER", signB = "SCORPIO", rating = 90,
            love = "Intense water bond. Cancer offers unconditional care; Scorpio offers profound loyalty. Both are psychic about each other’s moods. Highly protective.",
            communication = "Deep, indirect. Both read between lines. Rarely argue openly – instead, silent understanding or emotional tests.",
            finances = "Strong. Scorpio invests and controls; Cancer saves for home. Good at real estate. But Scorpio’s secrecy about money can trigger Cancer’s insecurity.",
            intimacy = "Transformative. Scorpio brings intensity; Cancer brings tenderness. Sex is emotional and healing. Jealousy can arise – both possessive.",
            conflict = "Scorpio’s silent rage meets Cancer’s tearful withdrawal. Can become cold war. Resolution requires vulnerability – Scorpio goes first.",
            parenting = "Fiercely protective, emotionally deep. Children feel safe but may be enmeshed.",
            longTermPotentialString = "90% – one of the best pairings overall. Only risk: mutual paranoia and emotional suffocation.",
            shadowWarning = "Scorpio’s tests (“I’ll pretend to be mad to see if you care”) hurt Cancer deeply. Cancer’s mood swings exhaust Scorpio."
        ),
        // 21. CANCER + PISCES
        "CANCER_PISCES" to CompatibilityInfo(
            signA = "CANCER", signB = "PISCES", rating = 85,
            love = "Dreamy water. Pisces brings magic; Cancer brings home. Both forgive easily. Very romantic, very creative.",
            communication = "Gentle, indirect, poetic. No harsh words. But both avoid conflict, so problems float unresolved.",
            finances = "Weak. Both have poor boundaries with money. Pisces spends on art/drugs; Cancer spends on family guilt. Need an earth sign manager.",
            intimacy = "Ethereal, tender, sometimes vague. Both can lose themselves in each other. Beautiful but can lack grounded passion.",
            conflict = "Crying, disappearing, blaming self. Neither takes responsibility. Issues dissolve into “it’s okay” without resolution.",
            parenting = "Overly permissive, artistic, emotionally intense. Children may lack structure but feel deeply loved.",
            longTermPotentialString = "85% – high for artistic, non-materialistic couples. Low for financial stability or practical life.",
            shadowWarning = "Mutual escapism. Both avoid reality until crisis (eviction, illness). Then they crumble."
        ),
        // 22. SCORPIO + SCORPIO
        "SCORPIO_SCORPIO" to CompatibilityInfo(
            signA = "SCORPIO", signB = "SCORPIO", rating = 60,
            love = "Intense, volcanic, all-or-nothing. Two obsessives. Magnetic attraction, deep loyalty, but also power struggles.",
            communication = "Telepathic, suspicious, probing. Both try to read the other’s hidden motives. Can be exhausting.",
            finances = "Powerful. Both control resources. Can be millionaires or criminals. Secrecy about money leads to trust issues. Joint accounts are battlegrounds.",
            intimacy = "Explosive, transformative, kinky. Best sex of any pairing. But jealousy and possessiveness can turn violent (emotionally or physically).",
            conflict = "Total war. Silent treatments, revenge, psychological manipulation. Both wait for the other to surrender. Can destroy each other.",
            parenting = "Intense, protective, demanding. Children feel both extreme love and extreme pressure.",
            longTermPotentialString = "60% – works only if both are highly evolved (therapy, spiritual practice). Otherwise, beautiful disaster ending in mutual destruction.",
            shadowWarning = "Mutual stalking, emotional blackmail, and “if I can’t have you, no one can.”"
        ),
        // 23. SCORPIO + PISCES
        "PISCES_SCORPIO" to CompatibilityInfo(
            signA = "SCORPIO", signB = "PISCES", rating = 95,
            love = "Soulmate potential. Scorpio provides structure for Pisces’ dreams; Pisces softens Scorpio’s intensity. Deep spiritual bond.",
            communication = "Pisces speaks in images; Scorpio translates into truth. No judgment. Both accept the dark and the light.",
            finances = "Pisces is hopeless; Scorpio takes over. Works if Scorpio handles money with transparency. If not, Pisces feels controlled.",
            intimacy = "Mystical and raw. Pisces dissolves boundaries; Scorpio penetrates. Sex is a religious experience. High risk of codependency.",
            conflict = "Scorpio’s anger scares Pisces; Pisces’ withdrawal enrages Scorpio. Resolution requires Scorpio to soften, Pisces to stand ground.",
            parenting = "Deeply intuitive, artistically rich, but Pisces parent may be too permissive, Scorpio too strict. Balance needed.",
            longTermPotentialString = "95% – one of the absolute best pairings in the zodiac. Only risk: Pisces’ escapism or Scorpio’s jealousy.",
            shadowWarning = "Scorpio may become savior; Pisces may become victim. Watch for unhealthy rescuer-rescued dynamic."
        ),
        // 24. PISCES + PISCES
        "PISCES_PISCES" to CompatibilityInfo(
            signA = "PISCES", signB = "PISCES", rating = 40,
            love = "Two fish swimming in the same dream. Boundless compassion, creativity, and shared delusion. Beautiful and dangerous.",
            communication = "Poetic, vague, indirect. Neither wants to define anything. Conversations float like clouds. Feelings are assumed, not stated.",
            finances = "Disaster. Double no boundaries. Both spend on charity, art, substances. Bills forgotten. Often bailed out by family.",
            intimacy = "Fluid, spiritual, often passive. Both can merge so completely that individual identity dissolves. Beautiful but unhealthy long-term.",
            conflict = "No fights. Just mutual sadness, tears, and avoidance. Nothing gets resolved. Both escape into art, sleep, or substances.",
            parenting = "Overly permissive, emotionally enmeshed. Children lack structure and may become parentified (taking care of Pisces parents).",
            longTermPotentialString = "40% – works only if one has strong earth placements (Virgo rising, Taurus moon). Otherwise, beautiful shipwreck.",
            shadowWarning = "Mutual addiction (alcohol, weed, fantasy). No one steers the boat."
        ),
        // 25. ARIES + TAURUS
        "ARIES_TAURUS" to CompatibilityInfo(
            signA = "ARIES", signB = "TAURUS", rating = 45,
            love = "Opposites attract. Aries speed, Taurus stillness. Initially exciting; long-term: Aries feels trapped, Taurus feels dragged.",
            communication = "Aries blurts; Taurus broods. Aries wants immediate resolution; Taurus needs time. Frequent frustration.",
            finances = "Conflict. Aries spends impulsively; Taurus saves. Joint accounts cause fights. Best: separate accounts with shared bill fund.",
            intimacy = "Aries wants quick, adventurous; Taurus wants slow, sensual. Can find middle ground with effort. Aries often tires Taurus out.",
            conflict = "Aries yells; Taurus withdraws. Aries feels ignored; Taurus feels attacked. Resolution: Aries must calm down, Taurus must speak up.",
            parenting = "Aries parent is fun, spontaneous; Taurus parent is consistent. Good balance if they respect each other.",
            longTermPotentialString = "45% – takes immense compromise. Works if Aries works 9-5 (earth schedule) and Taurus has hobbies (fire outlet).",
            shadowWarning = "Taurus’ stubbornness triggers Aries’ rage; Aries’ rage makes Taurus dig in deeper."
        ),

        // FIRE + EARTH (26-33)
        "ARIES_VIRGO" to CompatibilityInfo(
            signA = "ARIES", signB = "VIRGO", rating = 35,
            condensedText = "Aries’ chaos vs Virgo’s order. Virgo nags; Aries rebels. Sex is service-oriented or experimental, though Virgo might critique performance. Finances can be a disaster with divergent styles. Only works if Aries is in structured work (e.g., military) or Virgo is in the arts. Shadow warning: Mutual contempt risks eroding communication entirely."
        ),
        "ARIES_CAPRICORN" to CompatibilityInfo(
            signA = "ARIES", signB = "CAPRICORN", rating = 40,
            condensedText = "Aries initiates, Capricorn finishes. Great business synergy but romantic challenges: Capricorn sees Aries as immature; Aries sees Capricorn as boring. In intimacy, Capricorn's preference for schedules can frustrate Aries' spontaneity. Capricorn controls the purse strings, leading Aries to sneak-spending. Shadow warning: Chronic power struggle."
        ),
        "LEO_TAURUS" to CompatibilityInfo(
            signA = "LEO", signB = "TAURUS", rating = 65,
            condensedText = "Fixed fire + fixed earth. Loyal but stubborn. Leo wants applause; Taurus wants tangible results. Leo spends on flash, Taurus invests. In bed, Leo is performative and Taurus is deeply sensual – this can be great if Leo learns to go slow. Both refuse to yield in conflicts. Stable parenting with material focus. Shadow warning: Leo's grand ego can bruise Taurus' practical and grounded nature."
        ),
        "LEO_VIRGO" to CompatibilityInfo(
            signA = "LEO", signB = "VIRGO", rating = 50,
            condensedText = "Leo’s grandiosity vs Virgo’s humility. Virgo serves; Leo appreciates – until Virgo begins to criticize. Financially, Virgo saves Leo from potential overspending. Virgo aims to please in intimacy, and Leo is happy to direct. Conflict arises when Leo feels nagged and Virgo feels unappreciated. Long-term potential depends on Leo thanking Virgo daily. Shadow warning: Virgo’s perfectionism can kill Leo’s creative joy."
        ),
        "CAPRICORN_LEO" to CompatibilityInfo(
            signA = "LEO", signB = "CAPRICORN", rating = 70,
            condensedText = "An ambitious power couple with Leo's charm and Capricorn's discipline. Excellent finances if they coordinate shared goals. In bed, Capricorn respects Leo's passion, while Leo warms Capricorn's reserve. Conflict arises because Leo wants descriptive praise while Capricorn offers practical criticism. They make strict, proud parents. Shadow warning: Capricorn’s pessimism can drain Leo's bright spirit."
        ),
        "SAGITTARIUS_TAURUS" to CompatibilityInfo(
            signA = "SAGITTARIUS", signB = "TAURUS", rating = 30,
            condensedText = "Sag's absolute craving for freedom vs Taurus' deep root of stability. Sag wants global travel; Taurus wants home comforts. Finance is a battlefield of Sag's gambling vs Taurus' savings. In intimacy, Sag's adventurous styles clash with Taurus' routine. Taurus clings and Sag flees during conflicts. Shadow warning: Taurus feels abandoned while Sagittarius feels utterly suffocated."
        ),
        "SAGITTARIUS_VIRGO" to CompatibilityInfo(
            signA = "SAGITTARIUS", signB = "VIRGO", rating = 40,
            condensedText = "Mutable fire + mutable earth. Both are master adaptors but head in completely opposite directions. Virgo focuses on intricate details; Sag on the grand, big-picture. Virgo budgets meticulous funds, while Sag blows them on impulse. Virgo seeks service in intimacy; Sag seeks playful fun. Conflict: Virgo's worry annoys Sag, while Sag's recklessness terrifies Virgo. Shadow warning: Mutual harsh criticism can poison affection."
        ),
        "CAPRICORN_SAGITTARIUS" to CompatibilityInfo(
            signA = "SAGITTARIUS", signB = "CAPRICORN", rating = 55,
            condensedText = "The adventurous traveler meets the executive strategist. Capricorn admires Sag's optimism; Sag respects Capricorn’s real achievements. With finances, Capricorn manages while setting reasonable limits on Sag's spending. Intimacy requires compromise between Capricorn's need for schedule and Sag's love for spontaneity. Parenting balancing act: strict structure meets wild fun. Shadow warning: Capricorn sees Sag as irresponsible; Sag sees Capricorn as a joyless taskmaster."
        ),

        // FIRE + AIR (34-42)
        "ARIES_GEMINI" to CompatibilityInfo(
            signA = "ARIES", signB = "GEMINI", rating = 75,
            condensedText = "Fast talkers, fast energetic lovers. Boredom is completely nonexistent here. Financially, both can be impulsive, meaning risk. Intimacy is playful, highly communicative, and frequent. Conflict is resolved rapidly but sometimes avoids deeper themes. Shadow warning: Both can easily cycle through avoidance of long-term commitment."
        ),
        "ARIES_LIBRA" to CompatibilityInfo(
            signA = "ARIES", signB = "LIBRA", rating = 80,
            condensedText = "Opposites attract with magnetic intensity! Libra calms Aries down; Aries energizes Libra's decision-making. High bedroom polarity. In finances, separate accounts are best since Libra spends on beauty and Aries on sudden action. Conflict involves Aries fighting openly and Libra appeasing. Shadow warning: Libra can end up feeling completely steamrolled."
        ),
        "AQUARIUS_ARIES" to CompatibilityInfo(
            signA = "ARIES", signB = "AQUARIUS", rating = 65,
            condensedText = "Two passionate rebels. Aries is personal and direct; Aquarius is collective and ideological. Great activism potential and highly experimental sex. Financially, they represent erratic but exciting energy. Conflict: Aries' sudden hot anger meets Aquarius' icy detachment. Shadow warning: Aquarius may emotionally ghost or leave when Aries gets too volatile."
        ),
        "GEMINI_LEO" to CompatibilityInfo(
            signA = "GEMINI", signB = "LEO", rating = 85,
            condensedText = "Leo’s absolute warmth meets Gemini's sparkling wit—social royalty indeed! They can easily overspend since Leo buys status items and Gemini buys trendy variety. Intimacy is exceptionally playful, theatrical, and fun. Parenting styles are engaging and exciting. Shadow warning: Gemini’s playful flirting can quickly trigger Leo’s protective jealousy."
        ),
        "LEO_LIBRA" to CompatibilityInfo(
            signA = "LEO", signB = "LIBRA", rating = 90,
            condensedText = "The ultimate glamour couple. Both possess a supreme love for beauty, fine romance, and vibrant social scenes. They may spend heavily on luxuries, demanding strict budgeting. Intimacy is deeply graceful and highly passionate. Conflict: Libra sweeps things under the rug to avoid fire, while Leo demands open drama. Shadow warning: Leo's demanding ego vs Libra's indecisiveness."
        ),
        "AQUARIUS_LEO" to CompatibilityInfo(
            signA = "LEO", signB = "AQUARIUS", rating = 55,
            condensedText = "Opposites with distinct approaches. Leo wants warm admiration; Aquarius wants to shock the crowd. Initial attraction dissolves into power battles. Leo spends on premium self-image, while Aquarius donates to future/social causes. In intimacy, Leo is warm and passionate, while Aquarius stands detached. Shadow warning: Leo feels deeply unloved; Aquarius feels suffocated."
        ),
        "GEMINI_SAGITTARIUS" to CompatibilityInfo(
            signA = "GEMINI", signB = "SAGITTARIUS", rating = 80,
            condensedText = "Opposites as twin global adventurers. Both are mutable, adore travel, dynamic ideas, and absolute personal freedom. Finances are highly reckless, needing an earth-sign anchor. Sex is athletic, fun, with zero jealousy. Conflict is avoided because neither wants to settle. Shadow warning: Children may suffer from a severe lack of home stability."
        ),
        "LIBRA_SAGITTARIUS" to CompatibilityInfo(
            signA = "LIBRA", signB = "SAGITTARIUS", rating = 85,
            condensedText = "The meeting of optimism and grace. Highly powerful social pairing. Libra manages structured social investments, while Sag takes big risks. Intimacy is playful, light, and deeply affectionate. Parenting is cultured and highly permissive. Shadow warning: Both can easily agree to avoid processing difficult, heavy emotions."
        ),
        "AQUARIUS_SAGITTARIUS" to CompatibilityInfo(
            signA = "SAGITTARIUS", signB = "AQUARIUS", rating = 90,
            condensedText = "The absolute best fire-air matchup! Both value individual freedom and are focused on visionary ideas. Financial investments can be unconventional but highly brilliant. Intimacy is experimental and friendly. Fights are rare due to mutual space, but they can lack emotional glue. Shadow warning: Both may flee when raw emotional vulnerability appears."
        ),

        // FIRE + WATER (43-51)
        "CANCER_ARIES" to CompatibilityInfo(
            signA = "ARIES", signB = "CANCER", rating = 35,
            condensedText = "Aries is wildly impulsive; Cancer is highly emotional and cautious. Aries cuts through Cancer’s feelings with blunt remarks; Cancer’s changing moods can kill Aries’ energetic drive. Finances lead to war as Aries spends impulsively while Cancer saves for home. In intimacy, Aries wants fast activity and Cancer craves emotional bonding. Shadow warning: Aries views Cancer as manipulative; Cancer acts like Aries is cruel."
        ),
        "ARIES_SCORPIO" to CompatibilityInfo(
            signA = "ARIES", signB = "SCORPIO", rating = 40,
            condensedText = "Both are traditional warriors of Mars. Intense, primal physical attraction that dissolves into a power war. In intimacy, sex is explosive but naturally volatile. Finances are highly competitive. Conflicts pivot on who will dominate the other. Shadow warning: Great potential for deep emotional battles and psychological bruising if they are immature."
        ),
        "ARIES_PISCES" to CompatibilityInfo(
            signA = "ARIES", signB = "PISCES", rating = 45,
            condensedText = "Aries is direct and forward-facing; Pisces is indirect and mystical. Aries’ direct bluntness easily wounds delicate Pisces; Pisces’ escapism drives impatient Aries insane. Intimacy features Aries leading and Pisces melting into compliance. Finances are messy and lack boundaries. Shadow warning: Aries feels heavily burdened; Pisces feels actively bullied."
        ),
        "CANCER_LEO" to CompatibilityInfo(
            signA = "LEO", signB = "CANCER", rating = 50,
            condensedText = "A matching of Leo’s pride and Cancer’s need for deep emotional safety. Cancer nurtures Leo’s glowing ego, while Leo acts as Cancer’s protector. However, Leo's playful flirtations can hurt Cancer, while Cancer's fluctuating moods insult Leo's pride. Leo spends; Cancer reserves. Parenting is a mix of showmanship and smothering care. Shadow warning: A reciprocal struggle over unmet validation."
        ),
        "LEO_SCORPIO" to CompatibilityInfo(
            signA = "LEO", signB = "SCORPIO", rating = 55,
            condensedText = "Fixed fire meets fixed water. Both are fiercely loyal, but also highly possessive and jealous. A persistent power struggle for absolute leadership. Intimacy is incredibly intense. Financially, they build empires if aligned, but clash if not. Fights can turn into freezing silent wars. Shadow warning: Leo’s pride clashes violently with Scorpio’s demand for raw, stripped-down vulnerability."
        ),
        "LEO_PISCES" to CompatibilityInfo(
            signA = "LEO", signB = "PISCES", rating = 60,
            condensedText = "Leo brings vital confidence to Pisces; Pisces returns pure devotion to Leo. This is a beautiful, creative artistic matchup. Leo spends on luxury, while Pisces drifts on financial details. Sex is dreamy, highly romantic, and dramatic. Conflicts appear when Pisces' victim-mode annoys Leo or Leo's heavy demands exhaust Pisces. Shadow warning: Leo can end up exploiting Pisces' soft boundaries."
        ),
        "CANCER_SAGITTARIUS" to CompatibilityInfo(
            signA = "SAGITTARIUS", signB = "CANCER", rating = 35,
            condensedText = "Sag requires total global freedom; Cancer needs a secure emotional home. Sag's travel plans hurt domestic Cancer; Cancer's clinginess quickly suffocates Sag. Finances represent a collision of Sag's gambling and Cancer's savings. Sex is playful but can lack emotional synchrony. Shadow warning: Cancer feels completely abandoned; Sag feels locked in a trap."
        ),
        "SAGITTARIUS_SCORPIO" to CompatibilityInfo(
            signA = "SAGITTARIUS", signB = "SCORPIO", rating = 50,
            condensedText = "Sag's wild optimism meets Scorpio's absolute dark intensity. Scorpio demands profound, unwavering loyalty; Sag wants light adventure and curiosity. Scorpio seeks transformative intimacy, while Sag treats it as athletic fun. Scorpio controls the finances, which Sag resents. Shadow warning: Scorpio's possessive jealousy will eventually drive Sag away."
        ),
        "PISCES_SAGITTARIUS" to CompatibilityInfo(
            signA = "SAGITTARIUS", signB = "PISCES", rating = 65,
            condensedText = "Both share mutable energy and a love for big dreams. Pisces brings emotional depth; Sag brings philosophical hope. Idealistic, creative, but bad with daily details. Financially, neither understands boundaries. Intimacy is spiritual and boundaryless. Conflicts arise from Pisces' hurt feelings and Sag's blunt truths. Shadow warning: Extreme mutual escapism."
        ),

        // EARTH + AIR (52-60)
        "GEMINI_TAURUS" to CompatibilityInfo(
            signA = "TAURUS", signB = "GEMINI", rating = 35,
            condensedText = "Taurus is slow and steady; Gemini is mercurial and rapid. Gemini feels bored; Taurus feels anxious and on-edge. In finance, Taurus saves for the future; Gemini spends on current variety. In bed, Taurus seeks sensual touch, while Gemini seeks mental stimulation. Parenting styles can be deeply inconsistent. Shadow warning: Gemini labels Taurus as dull; Taurus sees Gemini as flaky."
        ),
        "LIBRA_TAURUS" to CompatibilityInfo(
            signA = "TAURUS", signB = "LIBRA", rating = 50,
            condensedText = "Both are Venus-ruled! Taurus is earthy physical beauty; Libra is airy intellectual aesthetics. Both adore luxury. Taurus wants a warm, comfortable home; Libra wants social gatherings and art. Finances are pleasant if coordinated. Intimacy is sensual and highly graceful. Conflict: Libra is indecisive; Taurus is incredibly stubborn. Shadow warning: Taurus' jealousy meets Libra's flirtations."
        ),
        "AQUARIUS_TAURUS" to CompatibilityInfo(
            signA = "TAURUS", signB = "AQUARIUS", rating = 30,
            condensedText = "Fixed earth meets fixed air—extraordinarily stubborn! Taurus wants comfortable routine; Aquarius wants shocking change and innovation. Taurus saves; Aquarius invests in wacky tech or causes. Intimacy is awkward: slow sensuality vs. experimental ideas. Parenting styles differ on discipline. Shadow warning: Taurus views Aquarius as unstable; Aquarius views Taurus as a boring dinosaur."
        ),
        "GEMINI_VIRGO" to CompatibilityInfo(
            signA = "GEMINI", signB = "VIRGO", rating = 45,
            condensedText = "Both are Mercury-ruled! Virgo is detail-oriented, practical Mercury; Gemini is playful, scattered Mercury. Great conversations but divergent goals. Virgo wants meticulous order; Gemini wants novelty. Virgo budgets; Gemini forgets bills. Intimacy works but lacks passion. Fights involve Virgo nagging and Gemini deflecting. Shadow warning: Virgo feels unappreciated; Gemini feels trapped.",
        ),
        "LIBRA_VIRGO" to CompatibilityInfo(
            signA = "VIRGO", signB = "LIBRA", rating = 55,
            condensedText = "Virgo is analytical and critical; Libra is diplomatic and peace-loving. Virgo’s blunt suggestions hurt sensitive Libra; Libra’s indecision drives Virgo crazy. Virgo saves; Libra spends on social status. Intimacy is clean and graceful. Parenting styles feature Virgo as strict and Libra as gentle. Shadow warning: Virgo labels Libra as superficial; Libra sees Virgo as a puritanical joy-killer."
        ),
        "AQUARIUS_VIRGO" to CompatibilityInfo(
            signA = "VIRGO", signB = "AQUARIUS", rating = 40,
            condensedText = "Virgo detail meets Aquarius' global vision. Highly logical but total clash on day-to-day methods. Virgo budgets; Aquarius disrupts standard plans. Intimacy is unique: Virgo is precise and Aquarius is experimental. Conflict involves Virgo criticising and Aquarius icing them out. Shadow warning: Virgo feels entirely ignored; Aquarius feels micromanaged."
        ),
        "CAPRICORN_GEMINI" to CompatibilityInfo(
            signA = "GEMINI", signB = "CAPRICORN", rating = 35,
            condensedText = "Capricorn is serious, legacy-focused; Gemini is youthful and scattered. Capricorn views Gemini as immature; Gemini sees Capricorn as a cold, boring boss. Capricorn controls finances; Gemini rebels against rules. Intimacy is a clash of schedule vs. spontaneity. Shadow warning: Capricorn feels utterly disrespected; Gemini feels emotionally suffocated."
        ),
        "CAPRICORN_LIBRA" to CompatibilityInfo(
            signA = "LIBRA", signB = "CAPRICORN", rating = 50,
            condensedText = "Capricorn is ambitious and pragmatic; Libra is social and aesthetic. Both are cardinal and strategic, making excellent business partners. Libra wants courtship; Capricorn wants rock-solid loyalty. Finances are highly organized and strong. Intimacy can be dry, requiring romance. Conflict: Libra is indecisive; Capricorn is impatient. Shadow warning: Libra feels starved; Capricorn feels manipulated."
        ),
        "AQUARIUS_CAPRICORN" to CompatibilityInfo(
            signA = "CAPRICORN", signB = "AQUARIUS", rating = 65,
            condensedText = "Both are Saturn-ruled. Capricorn is traditional rules; Aquarius is rebellious progressive systems. Shared work ethic but opposite goals. Capricorn saves; Aquarius invests in social tech/progress. Intimacy requires compromise between Capricorn's reserve and Aquarius' experimental ideas. Shadow warning: Capricorn labels Aquarius as reckless; Aquarius sees Capricorn as an old-fashioned authoritarian."
        ),

        // EARTH + WATER (61-69)
        "CANCER_TAURUS" to CompatibilityInfo(
            signA = "TAURUS", signB = "CANCER", rating = 90,
            condensedText = "Earth + water creates the ultimate cozy home. Taurus brings rock-solid stability; Cancer brings emotional depth and nurture. Both are savers, easily building substantial wealth together. Intimacy is sensual, warm, and highly tender. Excellent, secure parenting. Shadow warning: Both can easily become overly possessive, insular, and resistant to outside change."
        ),
        "SCORPIO_TAURUS" to CompatibilityInfo(
            signA = "TAURUS", signB = "SCORPIO", rating = 85,
            condensedText = "Opposites with absolute loyalty! Taurus is fixed earth, Scorpio is fixed water. Extremely intense magnetic chemistry. Financially, they act as powerful empire-builders (investments/real estate). Conflict involves Taurus' stubbornness colliding with Scorpio's fury, but both are fully committed. Shadow warning: Risk of extreme jealousy and control battles."
        ),
        "PISCES_TAURUS" to CompatibilityInfo(
            signA = "TAURUS", signB = "PISCES", rating = 80,
            condensedText = "Taurus grounds Pisces' floaty dreams; Pisces softens Taurus' material rigidity. Taurus handles finances securely. Intimacy is beautiful: combining Taurus' physical senses and Pisces' ethereal depth. Parenting is artistic, comforting, and stable. Shadow warning: Pisces’ escapism irritates Taurus; Taurus' blunt practicality hurts Pisces."
        ),
        "CANCER_VIRGO" to CompatibilityInfo(
            signA = "CANCER", signB = "VIRGO", rating = 85,
            condensedText = "Virgo serves; Cancer nurtures. The perfect operational domestic squad. Virgo structures the budget; Cancer saves for home. Intimacy is highly caring and emotionally healing. Parenting is detailed and deeply loving. Shadow warning: Virgo’s critical comments can bruise Cancer’s sensitive heart; Cancer’s sudden mood swings can destabilize analytical Virgo."
        ),
        "SCORPIO_VIRGO" to CompatibilityInfo(
            signA = "VIRGO", signB = "SCORPIO", rating = 80,
            condensedText = "Virgo is analytical; Scorpio is intensely investigative. Both are highly private and incredibly loyal. Excellent financial planning. Intimacy is intense and service-oriented. Conflict centers on Virgo's need for daily order vs. Scorpio's demand for total emotional control. Shadow warning: Mutual secrecy and growing suspicion if communication breaks."
        ),
        "PISCES_VIRGO" to CompatibilityInfo(
            signA = "VIRGO", signB = "PISCES", rating = 70,
            condensedText = "Opposites. Virgo detail meets Pisces' big dreams. Virgo helps Pisces manifest their ideas; Pisces teaches Virgo to relax. Virgo saves Pisces from financial chaos. Intimacy is a gorgeous blend of service and spiritual devotion. Conflicts: Virgo criticizes, Pisces escapes into isolation. Shadow warning: Virgo feels heavily burdened; Pisces feels unfairly judged."
        ),
        "CANCER_CAPRICORN" to CompatibilityInfo(
            signA = "CANCER", signB = "CAPRICORN", rating = 95,
            condensedText = "Polar opposites—among the strongest matches in the zodiac! Capricorn provides structure and protection; Cancer provides emotional home and warmth. Excellent traditional balance. Capricorn earns; Cancer cares for resource. Capricorn learns vulnerability, and Cancer gains structural safety. Balanced parenting. Shadow warning: Capricorn behaves like a workaholic; Cancer plays the passive-aggressive martyr."
        ),
        "CAPRICORN_SCORPIO" to CompatibilityInfo(
            signA = "SCORPIO", signB = "CAPRICORN", rating = 90,
            condensedText = "Both are ambitious, reserved, and extremely private. A true elite business and romantic power couple. They are natural empire-builders. Intimacy is intense but highly controlled. Conflicts can lead to a freezing cold war because neither yields easily. Shadow warning: Mutual distrust or intense paranoia if honesty is compromised."
        ),
        "CAPRICORN_PISCES" to CompatibilityInfo(
            signA = "CAPRICORN", signB = "PISCES", rating = 75,
            condensedText = "Capricorn provides physical structure to Pisces’ artistic dreams; Pisces infuses soul and artistic life into Capricorn’s cold ambitions. Capricorn manages money with care. Intimacy features Pisces melting Capricorn’s defenses. Parenting combines strict guidelines and gentle play. Shadow warning: Capricorn may control Pisces; Pisces may emotionally drain Capricorn."
        ),

        // AIR + WATER (70-78)
        "CANCER_GEMINI" to CompatibilityInfo(
            signA = "GEMINI", signB = "CANCER", rating = 40,
            condensedText = "Gemini is rational and airy; Cancer is emotional and watery. Gemini feels smothered by Cancer's nesting; Cancer feels ignored by Gemini's social flights. Gemini spends on variety; Cancer saves for home. Intimacy is light vs. highly emotional. parenting is chaotic. Shadow warning: Cancer feels abandoned; Gemini feels trapped."
        ),
        "GEMINI_SCORPIO" to CompatibilityInfo(
            signA = "GEMINI", signB = "SCORPIO", rating = 35,
            condensedText = "Gemini is light-hearted and social; Scorpio is deeply intense and private. Scorpio wants undivided depth; Gemini wants constant variety. Scorpio controls funds; Gemini hides spending. Intimacy can be frustrating as Scorpio seeks transformation and Gemini is easily distracted. Shadow warning: Scorpio feels betrayed; Gemini feels spied upon."
        ),
        "GEMINI_PISCES" to CompatibilityInfo(
            signA = "GEMINI", signB = "PISCES", rating = 50,
            condensedText = "Both are mutable signs. Highly creative, imaginative, and dreamy, but practically chaotic. Finances represent total chaos with no boundaries. Intimacy is playful, spiritual, and fluid. Communications are poetic but completely impractical in daily life. Shadow warning: They escape reality until crisis hits, with no anchor."
        ),
        "CANCER_LIBRA" to CompatibilityInfo(
            signA = "CANCER", signB = "LIBRA", rating = 55,
            condensedText = "Libra is highly social; Cancer is deeply domestic. Libra charms Cancer; Cancer nurtures Libra at home. Libra spends heavily on aesthetics; Cancer saves for family security. Intimacy is graceful and tender, but conflict resolution is bad: Libra avoids drama, while Cancer needs emotional depth. Shadow warning: Cancer feels unloved; Libra feels manipulated."
        ),
        "LIBRA_SCORPIO" to CompatibilityInfo(
            signA = "LIBRA", signB = "SCORPIO", rating = 45,
            condensedText = "Libra is airy and light; Scorpio is watery and dark. Scorpio’s intense gaze scares Libra; Libra’s indecisive flirting frustrates Scorpio. Scorpio controls the finances, which Libra resents. Intimacy is passionate but emotionally tense. Conflict features Scorpio's jealousy meeting Libra's charming behavior. Shadow warning: Libra feels controlled; Scorpio feels betrayed."
        ),
        "LIBRA_PISCES" to CompatibilityInfo(
            signA = "LIBRA", signB = "PISCES", rating = 65,
            condensedText = "Beautiful artistic matching ruled by Venus/Neptune dynamics! Both are romantic, gentle, and highly creative. They can easily overspend on beauty and comfort, needing an earthy financial anchor. Intimacy is dreamy and elegant. Conflicts go unaddressed because neither likes confrontation. Shadow warning: Mutual avoidance of heavy reality."
        ),
        "CANCER_AQUARIUS" to CompatibilityInfo(
            signA = "CANCER", signB = "AQUARIUS", rating = 35,
            condensedText = "Aquarius is detached and analytical; Cancer is emotionally close and sensitive. Cancer needs tight connection; Aquarius demands wide personal space. Aquarius spends on technology/progress; Cancer saves for home. In bed, Aquarius is cool and experimental, while Cancer seeks warm bonding. Shadow warning: Cancer feels abandoned; Aquarius feels suffocated."
        ),
        "AQUARIUS_SCORPIO" to CompatibilityInfo(
            signA = "SCORPIO", signB = "AQUARIUS", rating = 40,
            condensedText = "Both are fixed signs, creating a stubborn power battle! Aquarius wants absolute personal freedom; Scorpio wants deep emotional control. In bed, chemistry is experimental and intensely focused if they find balance. Fights center on Scorpio's jealousy meeting Aquarius' icy detachment. Shadow warning: Mutual suspicion and emotional walls."
        ),
        "AQUARIUS_PISCES" to CompatibilityInfo(
            signA = "AQUARIUS", signB = "PISCES", rating = 70,
            condensedText = "Both are highly idealistic and unique! Aquarius brings scientific vision; Pisces brings deep, soulful compassion. Ideal for art, humanitarian work, and activism. Finances are neglected as both are detached. Intimacy is fluid and dreamy. Conflict: Pisces’ raw sensitivity meets Aquarius' cool logic. Shadow warning: Aquarius dismisses Pisces' feelings; Pisces drowns Aquarius in emotion."
        )
    )
}
