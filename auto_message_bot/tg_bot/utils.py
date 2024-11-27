import re


def check_timing(text) -> bool:
    '''检查推送时间。正则表达式，验证时间格式 HH:MM'''
    time_pattern = r"^(?:[01]?[0-9]|2[0-3]):([0-5][0-9])$"

    # 匹配时间格式
    match = re.match(time_pattern, text)

    if match:
        hours = int(text[:2])  # 提取小时部分
        minutes = int(text[3:])  # 提取分钟部分

        if 0 <= hours <= 23 and 0 <= minutes <= 59:
            # 如果符合时间范围
            return True
        else:
            return False
    else:
        return False


def check_button_url(text) -> bool:
    '''检查按钮链接是否以 http:// 或 https:// 开头'''
    url_pattern = r"^(http|https)://"
    return bool(re.match(url_pattern, text))


def create_markup(button_list):
    from telebot import types
    markup = types.InlineKeyboardMarkup()

    # 按照每两对按钮分组
    for i in range(0, len(button_list), 2):
        # 如果有两对按钮，添加到同一行
        if i + 1 < len(button_list):
            markup.add(
                types.InlineKeyboardButton(text=button_list[i][0], url=button_list[i][1]),
                types.InlineKeyboardButton(text=button_list[i + 1][0], url=button_list[i + 1][1])
            )
        else:
            markup.add(types.InlineKeyboardButton(text=button_list[i][0], url=button_list[i][1]))

    return markup


