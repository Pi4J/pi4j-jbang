package helper.lcd;

/**
 * Enumeration with most important and used symbols. Resolves ASCII character to the LCD Display characters table
 */
public enum LcdSymbol {
    ZERO('0', 0x30),
    ONE('1', 0x31),
    TWO('2', 0x32),
    THREE('3', 0x33),
    FOUR('4', 0x34),
    FIVE('5', 0x35),
    SIX('6', 0x36),
    SEVEN('7', 0x37),
    EIGHT('8', 0x38),
    NINE('9', 0x39),

    A('A', 0x41),
    B('B', 0x42),
    C('C', 0x43),
    D('D', 0x44),
    E('E', 0x45),
    F('F', 0x46),
    G('G', 0x47),
    H('H', 0x48),
    I('I', 0x49),
    J('J', 0x4A),
    K('K', 0x4B),
    L('L', 0x4C),
    M('M', 0x4D),
    N('N', 0x4E),
    O('O', 0x4F),
    P('P', 0x50),
    Q('Q', 0x51),
    R('R', 0x52),
    S('S', 0x53),
    T('T', 0x54),
    U('U', 0x55),
    V('V', 0x56),
    W('W', 0x57),
    X('X', 0x58),
    Y('Y', 0x59),
    Z('Z', 0x5A),

    a('a', 0x61),
    b('b', 0x62),
    c('c', 0x63),
    d('d', 0x64),
    e('e', 0x65),
    f('f', 0x66),
    g('g', 0x67),
    h('h', 0x68),
    i('i', 0x69),
    j('j', 0x6A),
    k('k', 0x6B),
    l('l', 0x6C),
    m('m', 0x6D),
    n('n', 0x6E),
    o('o', 0x6F),
    p('p', 0x70),
    q('q', 0x71),
    r('r', 0x72),
    s('s', 0x73),
    t('t', 0x74),
    u('u', 0x75),
    v('v', 0x76),
    w('w', 0x77),
    x('x', 0x78),
    y('y', 0x79),
    z('z', 0x7A),

    EXCLAMATION_MARK('!', 0x21),
    DOUBLE_QUOTE('\"', 0x22),
    NUMBER_SIGN('#', 0x23),
    DOLLAR('$', 0x24),
    PERCENT('%', 0x25),
    AMPERSAND('&', 0x26),
    QUOTE_SINGLE('\'', 0x27),
    PARENTHESIS_LEFT('(', 0x28),
    PARENTHESIS_RIGHT(')', 0x29),
    ASTERISK('*', 0x2A),
    PLUS('+', 0x2B),
    COMMA(',', 0x2C),
    HYPHEN('-', 0x2D),
    PERIOD('.', 0x2E),
    SLASH('/', 0x2F),
    COLON(':', 0x3A),
    SEMICOLON(';', 0x3B),
    LESS('<', 0x3C),
    EQUAL('=', 0x3D),
    GREATER('>', 0x3E),
    QUESTION('?', 0x3F),
    AT('@', 0x40),
    BRACKET_LEFT('[', 0x5B),
    YEN('¥', 0x5C),
    BRACKET_RIGHT(']', 0x5D),
    CARET('^', 0x5E),
    UNDERSCORE('_', 0x5F),
    GRAV('`', 0x60),
    BRACE_LEFT('{', 0x7B),
    BAR('|', 0x7C),
    BRACE_RIGHT('}', 0x7D),
    ARROW_RIGHT('→', 0x7E),
    ARROW_LEFT('←', 0x7F),
    SQUARE('□', 0xA1),
    TOP_LEFT_CORNER('⌜', 0xA2),
    BOTTOM_RIGHT_CORNER('⌟', 0xA3),
    SMALL_BACKSLASH('﹨', 0xA4),
    KATAKANA_MIDPOINT('･', 0xA5),
    SMALL_ALPHA('α', 0xE0),
    LATIN_SMALL_A_WITH_DIAERESIS('ä', 0xE1),
    BIG_BETA('β', 0xE2),
    SMALL_EPSILON('ε', 0xE3),
    SMALL_MY('μ', 0xE4),
    SMALL_SIGMA('σ', 0xE5),
    SMALL_RHO('ρ', 0xE6),
    SQUARE_ROOT('√', 0xE8),
    LATIN_SMALL_O_WITH_DIAERESIS('ö', 0xEF),
    BIG_THETA('ϴ', 0xF2),
    INFINITY_SIGN('∞', 0xF3),
    BIG_OMEGA('Ω', 0xF4),
    LATIN_SMALL_U_WITH_DIAERESIS('ü', 0xF5),
    BIG_SIGMA('∑', 0xF6),
    SMALL_PI('π', 0xF7),
    SHIN('Ⴘ', 0xF9),
    TSHE('Ћ', 0xFB),
    DIVISION('÷', 0xFD),
    SPACE(' ', 0xFE),
    BLACKBOX('⏹', 0xFF),

    OWN_CHARACTER_1('\1', 0x01),
    OWN_CHARACTER_2('\2', 0x02),
    OWN_CHARACTER_3('\3', 0x03),
    OWN_CHARACTER_4('\4', 0x04),
    OWN_CHARACTER_5('\5', 0x05),
    OWN_CHARACTER_6('\6', 0x06),
    OWN_CHARACTER_7('\7', 0x07);

    /**
     * ASCII character to which this symbol belongs to or ? if no ASCII mapping is available
     */
    private final int ascii;

    /**
     * Byte representing the ASCII character on the LCD Display
     */
    private final int code;

    /**
     * Creates a new symbol associated to a specific ASCII character
     *
     * @param ascii ASCII character to be associated with
     * @param code  byte representing the chosen ASCII character on the LCD Display
     */
    LcdSymbol(int ascii, int code) {
        this.ascii = ascii;
        this.code = code;
    }

    /**
     * Method to search a the corresponding byte to an ASCII sign. Returns a ? if a symbol is not found
     *
     * @param c ASCII Symbol
     * @return Byte needed to display the Symbol on the LCD Display
     */
    public static int getByChar(char c) {
        for (LcdSymbol symbol : LcdSymbol.values()) {
            if (symbol.ascii == c) {
                return symbol.code;
            }
        }
        return QUESTION.code;
    }
}
