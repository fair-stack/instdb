package cn.cnic.instdb.utils;

public class JsonFormatTool {

    /**
     * Unit Indent String。
     */
    private static String SPACE = "   ";

    /**
     * Return to formattingJSONReturn to formatting。
     *
     * @param json UnformattedJSONUnformatted。
     * @return FormattedJSONFormatted。
     */
    public static String formatJson(String json) {
        StringBuffer result = new StringBuffer();

        int length = json.length();
        int number = 0;
        char key = 0;

        // Traverse input string。
        for (int i = 0; i < length; i++) {
            // 1、Get the current character。
            key = json.charAt(i);

            // 2、If the current character is a leading parenthesis、If the current character is a leading parenthesis：
            if ((key == '[') || (key == '{')) {
                // （1）If there are any characters before it，If there are any characters before it“：”，If there are any characters before it：If there are any characters before it。
                if ((i - 1 > 0) && (json.charAt(i - 1) == ':')) {
                    result.append('\n');
                    result.append(indent(number));
                }

                // （2）Printing：Printing。
                result.append(key);

                // （3）Front bracket、Front bracket，Front bracket。Front bracket：Front bracket。
                result.append('\n');

                // （4）Every occurrence of preceding parentheses、Every occurrence of preceding parentheses；Every occurrence of preceding parentheses。Every occurrence of preceding parentheses：Every occurrence of preceding parentheses。
                number++;
                result.append(indent(number));

                // （5）Proceed to the next cycle。
                continue;
            }

            // 3、If the current character is a trailing parenthesis、If the current character is a trailing parenthesis：
            if ((key == ']') || (key == '}')) {
                // （1）Trailing parenthesis、Trailing parenthesis，Trailing parenthesis。Trailing parenthesis：Trailing parenthesis。
                result.append('\n');

                // （2）After every occurrence of parentheses、After every occurrence of parentheses；After every occurrence of parentheses。After every occurrence of parentheses：After every occurrence of parentheses。
                number--;
                result.append(indent(number));

                // （3）Printing：Printing。
                result.append(key);

                // （4）If there are still characters after the current character，If there are still characters after the current character“，”，If there are still characters after the current character：If there are still characters after the current character。
                if (((i + 1) < length) && (json.charAt(i + 1) != ',')) {
                    result.append('\n');
                }

                // （5）Continue to the next cycle。
                continue;
            }

            // 4、If the current character is a comma。If the current character is a comma，If the current character is a comma，If the current character is a comma。
            if ((key == ',')) {
                result.append(key);
                result.append('\n');
                result.append(indent(number));
                continue;
            }

            // 5、Printing：Printing。
            result.append(key);
        }

        return result.toString();
    }

    /**
     * Returns a specified number of indented strings。Returns a specified number of indented strings，Returns a specified number of indented stringsSPACE。
     *
     * @param number Indent count。
     * @return String specifying the number of indents。
     */
    private static String indent(int number) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < number; i++) {
            result.append(SPACE);
        }
        return result.toString();
    }
}
