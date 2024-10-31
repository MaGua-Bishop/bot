import json


class JoinGroupMessage:
    def __init__(self, chat_id, message_id):
        self.chat_id = chat_id
        self.message_id = message_id

    def get_chat_id(self):
        return self.chat_id

    def set_chat_id(self, chat_id):
        self.chat_id = chat_id

    def get_message_id(self):
        return self.message_id

    def set_message_id(self, message_id):
        self.message_id = message_id


class GroupButton:
    def __init__(self, name, url):
        self.name = name
        self.url = url

    def get_name(self):
        return self.name

    def get_url(self):
        return self.url


    def to_dict(self):
        return {'name': self.name, 'url': self.url}


class Data:
    def __init__(self, chat_id, money, join_group_message, group_buttons):
        self.chat_id = chat_id
        self.money = money
        self.join_group_message = join_group_message
        self.group_buttons = group_buttons

    @classmethod
    def from_json(cls, json_data):
        join_group_message = JoinGroupMessage(**json_data['join_group_message'])
        group_buttons = [GroupButton(**button) for button in json_data['group_button']]
        return cls(
            chat_id=json_data['chat_id'],
            money=json_data['money'],
            join_group_message=join_group_message,
            group_buttons=group_buttons
        )

    def to_json(self):
        return {
            'chat_id': self.chat_id,
            'money': self.money,
            'join_group_message': {
                'chat_id': self.join_group_message.get_chat_id(),
                'message_id': self.join_group_message.get_message_id(),
            },
            'group_button': [button.to_dict() for button in self.group_buttons]
        }

    def get_chat_id(self):
        return self.chat_id

    def set_chat_id(self, chat_id):
        self.chat_id = chat_id

    def get_money(self):
        return self.money

    def set_money(self, money):
        self.money = money

    def set_join_group_message(self, chat_id, message_id):
        self.join_group_message = JoinGroupMessage(chat_id, message_id)

    def get_join_group_message(self):
        return self.join_group_message

    def get_group_buttons(self):
        return self.group_buttons

    def set_group_buttons(self, group_buttons):
        self.group_buttons = group_buttons
    def remove_group_button(self, name):
        self.group_buttons = [button for button in self.group_buttons if button.get_name() != name]


# 获取文件数据
def get_set_file_data(file_path='tg_bot/utils/set_file.json') -> Data:
    try:
        with open(file_path, 'r') as f:
            json_str = f.read()
        data_dict = json.loads(json_str)
        data = Data.from_json(data_dict)
        return data
    except FileNotFoundError:
        print(f"文件 {file_path} 未找到。")
        return None
    except json.JSONDecodeError:
        print("JSON 解码错误，请检查文件格式。")
        return None


# 修改文件数据
def save_set_file_data(data, file_path='tg_bot/utils/set_file.json') -> bool:
    try:
        with open(file_path, 'w') as f:
            json.dump(data.to_json(), f, indent=4)
        return True
    except Exception as e:
        print(f"保存数据时发生错误: {e}")
        return False

import os

print("当前工作目录:", os.getcwd())
