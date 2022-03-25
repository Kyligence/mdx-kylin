## Predefined measure formats and measure format characters



#### Predefined Measure Formats

| <span style="white-space:nowrap">**Format name** &nbsp;&nbsp;</span> | **Description**                                              |
| :----------------------------------------------------------- | ------------------------------------------------------------ |
| Unformatted                                                  | No formatting.                                               |
| Number                                                       | Can choose whether to take thousands of separators, negative format, can configure the number of digits after the decimal separator, at least one digit to the left of the decimal separator. |
| Currency                                                     | Can choose whether to take thousands of separators, negative format, can configure the number of digits after the decimal separator, currency symbol is based on system locale settings. |
| Percentage                                                   | Displays the number multiplied by 100 with a percent sign (%) appended to the right, can configure the number of digits after the decimal separator. |



#### Measure Format Expression

A user-defined format expression for numbers can have anywhere from one to four sections separated by semicolons. 

| <span style="white-space:nowrap">**Usage** &nbsp;&nbsp;</span> | **Description**                                              |
| :----------------------------------------------------------- | ------------------------------------------------------------ |
| One section                                                  | The format expression applies to all values.                 |
| Two sections                                                 | The first section applies to positive values and zeros, the second to negative values. |
| Three sections                                               | The first section applies to positive values, the second to negative values, and the third to zeros. |

The following example has two sections. The first section defines the format for positive values and zeros, and the second section defines the format for negative values.

```
#,##0;(-#,##0)
```

The following example has three sections. The first section defines the format for positive values, the second section defines the format for negative values, and displays "Zero" if the value is zero.

```
$#,##0;(-$#,##0);\Z\e\r\o
```



#### Measure Format Characters

| **Character** | **Description** |
| :-----: | ---- |
| 0 | Represents a digit placeholder that displays a digit or a zero (0).<br/><br/>If the number has a digit in the position where the zero appears in the format string, the formatted value displays the digit. Otherwise, the formatted value displays a zero in that position.<br/><br/>If the number has fewer digits than there are zeros (on either side of the decimal) in the format string, the formatted value displays leading or trailing zeros.<br/><br/>If the number has more digits to the right of the decimal separator than there are zeros to the right of the decimal separator in the format expression, the formatted value rounds the number to as many decimal places as there are zeros.<br/><br/>If the number has more digits to the left of the decimal separator than there are zeros to the left of the decimal separator in the format expression, the formatted value displays the additional digits without modification. |
| # | Represents a digit placeholder that displays a digit or nothing.<br/><br/>If the expression has a digit in the position where the number sign (#) appears in the format string, the formatted value displays the digit. Otherwise, the formatted value displays nothing in that position.<br/><br/>The number sign (#) placeholder works like the zero (0) digit placeholder except that leading and trailing zeros are not displayed if the number has the same or fewer digits than there are # characters on either side of the decimal separator in the format expression. |
| . | Represents a decimal placeholder that determines how many digits are displayed to the left and right of the decimal separator.<br/><br/>If the format expression contains only number sign (#) characters to the left of the period (.), numbers smaller than 1 start with a decimal separator. To display a leading zero displayed with fractional numbers, use zero (0) as the first digit placeholder to the left of the decimal separator. |
| % | Represents a percentage placeholder. The expression is multiplied by 100. The percent character (**%**) is inserted in the position where the percentage appears in the format string. |
| , | Represents a thousand separator that separates thousands from hundreds within a number that has four or more places to the left of the decimal separator. |
| E- E+ e- e+ | Represents scientific format.<br/><br/>If the format expression contains at least one digit placeholder (0 or #) to the right of E-, E+, e-, or e+, the formatted value displays in scientific format and E or e is inserted between the number and the number's exponent. The number of digit placeholders to the right determines the number of digits in the exponent. Use E- or e- to include a minus sign next to negative exponents. Use E+ or e+ to include a minus sign next to negative exponents and a plus sign next to positive exponents. |
| + - $  (   ) | Displays a literal character.<br /><br />Recommended usage：<br />+：for positive number<br />- or ( )：for negative number<br />$：for currency symbol |
| \ | Displays the next character in the format string.<br/><br/>To display a character that has special meaning as a literal character, put a backslash (\) before the character. The backslash itself is not displayed. Using a backslash is the same as enclosing the next character in double quotation marks. To display a backslash, use two backslashes (\\). Examples of characters that cannot be displayed as literal characters include the following characters:<br /><br /># 0 % E e , . |
