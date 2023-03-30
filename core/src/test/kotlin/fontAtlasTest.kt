
''''import com.aayushatharva.brotli4j.Brotli4jLoader
import com.aayushatharva.brotli4j.decoder.Decoder
import com.aayushatharva.brotli4j.decoder.DecoderJNI
import glm_.i
import imgui.font.FontAtlas
import imgui.font.decode
import kool.lib.indices
import java.math.BigInteger
import kotlin.test.Test

class fontAtlasTest {

    @Test
    fun `glyph backing`() {

        val atlas = FontAtlas()
        atlas.build()

        Brotli4jLoader.ensureAvailability()

        val range = (0xD7FF + 1 - 0x0000) + (0xFFFF + 1 - 0xE000) + (Char.MAX_HIGH_SURROGATE.i + 1 - Char.MIN_SURROGATE.i) * (Char.MAX_SURROGATE.i + 1 - Char.MIN_LOW_SURROGATE.i)
        var value = BigInteger.ZERO
        var i = 0
        while (i < encodedCompressedTexPixelsAlpha8.length) {
            val s = when (val c = encodedCompressedTexPixelsAlpha8[i++]) {
                in Char.MIN_SURROGATE..Char.MAX_SURROGATE -> c.toString() + encodedCompressedTexPixelsAlpha8[i++]
                else -> c.toString()
            }
            value = value * range.toBigInteger() + decode(s).toBigInteger()
        }

        val texPixelsAlpha8 = Decoder.decompress(value.toByteArray()).run {
            check(resultStatus == DecoderJNI.Status.DONE)
            decompressedData
                           }

        for (j in atlas.texPixelsAlpha8!!.indices) {
            if (texPixelsAlpha8[j] != atlas.texPixelsAlpha8!![j])
                error("at $j, ${texPixelsAlpha8[j]} != " + atlas.texPixelsAlpha8!![j])
        }
//        for (i in atlas.texPixelsAlpha8!!.indices step 8) {
//            val a = atlas.texPixelsAlpha8!![i].toLong() and 0xFF
//            val b = atlas.texPixelsAlpha8!![i + 1].toLong() and 0xFF
//            val c = atlas.texPixelsAlpha8!![i + 2].toLong() and 0xFF
//            val d = atlas.texPixelsAlpha8!![i + 3].toLong() and 0xFF
//            val e = atlas.texPixelsAlpha8!![i + 4].toLong() and 0xFF
//            val f = atlas.texPixelsAlpha8!![i + 5].toLong() and 0xFF
//            val g = atlas.texPixelsAlpha8!![i + 6].toLong() and 0xFF
//            val h = atlas.texPixelsAlpha8!![i + 7].toLong() and 0xFF
//            val long = (h shl 56) or (g shl 48) or (f shl 40) or (e shl 32) or (d shl 24) or (c shl 16) or (b shl 8) or a
////            println("[${i / 8}] $long")
//            if (texPixelsAlpha8[i / 8] != long)
//                System.err.println("at ${i / 8}, ${texPixelsAlpha8[i / 8]} != " + long)
//        }
    }

    // ## Test ImFontAtlas clearing of input data (#4455, #3487)
    @Test
    fun `clearing of input data`() {

        val atlas = FontAtlas()
        atlas.build()
        atlas.isBuilt `should be` true

        atlas.clearTexData()
        atlas.isBuilt `should be` true

        atlas.clearInputData()
        atlas.fonts.size `should be greater than` 0
//        #if IMGUI_VERSION_NUM > 18407
        atlas.isBuilt `should be` true
//        #endif

        atlas.clearFonts()
        atlas.fonts.size `should be` 0
        atlas.isBuilt `should be` false

        atlas.build()  // Build after clear
        atlas.build()  // Build too many times
    }

    companion object {
        val encodedCompressedTexPixelsAlpha8 = " 񩣝񺐷𺗨🩐ힰ𴃑💨𝏤󮸌񆣒񝠚󶷓򈍨󐠐𯲴󴪡񊌯󯮓򶿱񦕞򷫊󳞐󺏧𢕍򚘜򙗬󊆈򞖙󷂌𣉆󶱩𞴹򙙱򎰄𮵋󄑩򢒊񏐆򥥼󋂊𡣒򴮭󷃛಍򑅉𓙏񚁾񡜰򎵘򛤉𚘤񲚎󴞻򠄥ﺼ򮴯𢻛󰘖𚻐񸮁񫥽򁝿𨂞񭶤𛉊񽝓򂴶󰿎𶨇񟷴򩉾󃖆𢑹ퟻ񪐗󘴽󓧩򗹫󜕩𮀒񤧤񫜛񘵟🌅񔷢􇔽󞱎򯼐𮷻񮻉򌀪񖧝񭘐򼙛򣺏񐛞򷈶󓉿󣒏䜯󞳪􇲰񧩨𒢈𭲨󷔵𾿐򞖵򴆦𞜉񥇐嚺􆍒򈴋򛇱񮍳󸝐򫷜𾋌𴄝򇖟󕑵󞝡򌩵񁷒󵁖󉙀󧂊􉿠񋟐쌊򮖓򡍑񲘬愆웊𸍶󽳿􀀲񯾑ز򰌳󂱂񫤊󤩮򨹈􀙷񇸑񒧚򸕭𔘖򹡸𚕬񕈞򅃾򝦋񡁌򈱪𠮝󍻑񥵋𜹏񞯴􂱚򌋬򛺦񀁓걦򀬠򻫴񰮞򄫱󓊰򬃥𲵩򌔙򢪅򮸽􌖺񞉀󷫺򅇚񿭅񨂘󇝝񭢨򣆃񯅝􀱤򶇇򔇮􆀋󰰔􄉤􁄧󙱽򃕼񲂋􅊜򭺪󖙿𣬹􆝼鿼􎙹󲏧󼳁󓻮򏽑룥񰘽򄅶򺞙𮱧𨿝缧򭶻𷣁򅂱𵜳򐆲񙹇񵼄􇵘𱾤󶂓񋨳󼿝񻃚󖶝疂󔜌󲹧𬟖󬐆󋨐򪍕𖥳󩜢򚦕󷬌񼻃󽉺󡞛𽻘󛆻򻨌𜸍񘇾򿿺뮋󰠒񨲌ﲷ󍈮󜋎뿅򶡻𓯯䤨󆤗񗊅􁦷󿞯󦴩򡒉򋬉񌌹󿬍񹇷񷥩򭠶򢹑񗭋񂢝񾬡󐏔󰉽񙠤󪧧󘟳񧭎󵶬𨯵󺐝򍬛쇁􋟋񍞼盂󨳯𬃾񂎰򔣓񭩩󈠆򄳥򮋑𔮼𤷁󿷌𵅶𖙵򝬳񛕎󷇬򔰧􆻜噹󩰋󡿖򦛘񔳗𚍞򅪥񨾴򍬨󔓒󉔍𞁓🧅󁁛􉀛򯦔𻈁󜏪򪸅󽩝􏌃󧾮򒞋򈫩𹤤󟲵󪘍걘򪀢򻼢񓸬򥊖󉱍򋍶񈠡񵵞𓜻񧩽򐈫닮񇏙󛀘򡨤򻧨򾧢񤕱򿪬񲑍󊅪𛟝𜾡ᤵ󹤾𽶕򧾁񇆵򀰞񊚲󫂛򏡝󓇹󭽤򄷯𦼐𘖚򞑫𥁡񑤗񷫣񳈈񊸹򓓰󈗲𸂏󡭖𩨇묋𨭘񋻲󥖎􃏓𖛧񗊄䦥𹩺񏟅񼢣Ѷ򩃖󰔷􀑄􇋼􊲣򢰽򽠅񀘺𹏱򊮁󱽁򏙴󜂦󗼚󸭦𨟚𐁬󄫮𺻉󨧾񯵅񐪁􋛭낵􂻼󸚔󪌑􀋚򪆴񤝡򲭃򼎖􁉢𧩀򌕼񦩠󗎔򰲂𴶈𾜫𲺱󯀪󫏪񡩔⪪񣓐򴇉𒉐񶋄򉡮𼴌𧍉񽌔򎋀󅭉򯼍󘐧񨏝𽈑𱠧򤑍⠌񁎇񾼣摉񟌂󈆱拚󚭫󍫢񕪥򐁏󒍴󈙘򟍠󕳛󣤂򯫅􏞲󮯑󛤡󹺷񹮎󀌤򥜬􂄼񎜌󳈾񈘮򭯞󷯅𿭨󇖘󊦄񠄠󑟃񝐨󌕝򴩙񫓀𴣎񽎅𲅕򎗶񕙭񬌵񺔺𮢹񁟅􊟞򲿉󊺞󚄽𚿖󔓒񸗝񷽤𓭷򶩕𗌸􊵠񧏜򷘢򃹇򿇻򏩿򯏲𩩻𱌉򮁣񈩣򲏧򞵨𛬛𝠪񩈷񛟸󠹘󣓽񡦚⦄𕙮򅧏񚆂𰓸񫭢󟼉󒾾󬱶𸡿󿈂򹎈񈨜򊙭񣋍󏴥򷁤𺻜򭏏񧖤毉󌞮򻖱򈾂򷝖󽈽󙌷򊫪𚏅􇷡񺦈⺷񥙸񠗟񓹈򍏢􀃦兣񑳜񕛽𷭪򁜳򥨏򐶽񅇼󷘮򃧫󥙙󔭀򮕋򿟉𥸽󄄀󬩮򲪽󨲻񲤕񶯧򙞃񵑶󒚲󿻋󾕚򬩞򏘤󰒶ឋ𢼂񢾫򉪍񲾅񛅾𥎅𝴄󫈳񽰐򃳩󺅴񾒒򧽵𛄞𰞛򤬃󐬋졇􏓜󃁔󈢠𔩁󞆏󅲽򜳳⍡񽴰񟲝񶁦􌢛𽅃􈩻񹄿򘼲񾕽󆶌󜐇󜫼򓑠򖄎񷩈񻪸𽒏𗲒񗆽񤐣󔹓󋚵𰠑󭫄𿃋󖦇򓄨񠲄񉚨񜿛󾀂񥨯򝮮􀔥𪃈񜄖񲃗󛁃򘗈򌊄𧿙󵤃򗿟򌞴򣚙ꠔ􇮊󰕀𬠷񌟹񹡭񥬾󩊕𠡗񧙌󷴜򌡭򶈞񫱨󚻤󄡨󜬑񶐻󰾢񄲜􂵑򽯨񊄛󑩄󑸼󍍵􂌁"
    }
}