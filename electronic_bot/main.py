# import requests
#
#
# def change_energy_webhook_tron(address: str, action="remove", tip_type="USDT",
#                                webhookUrl="https://tgfrw.com/trx/transaction/energy_address"):
#     """添加回调地址"""
#
#     url = "http://5.45.72.79:8000/create-address-tracker/"
#     if action == "remove":
#         url = "http://5.45.72.79:8000/delete-address-tracker/"
#     data = {
#         "address": address,
#         "tip_type": "delete",
#         "webhookUrl": webhookUrl
#     }
#     try:
#         result = requests.post(url, json=data).text
#         print(result)
#     except Exception as e:
#         print(e)
#
#
# if __name__ == '__main__':
#     address = "TVgX88b7ndx1nTCGQnFFE9Rm5fgg1rKs9P"
#     webhookUrl = "bot2.tgfrw.com/tgbot/electronic_bot/recharge"
#     change_energy_webhook_tron(address,webhookUrl)
