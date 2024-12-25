import requests
import hashlib
from urllib.parse import urlencode
from django.conf import settings


class OkayPay:
    def __init__(self):
        self.id = 13620
        self.token = 'VfeikGoUqgRuyCsEGIK1SFh0t24JwZn'
        self.api_url = 'https://okpay.xgram.me/shop/'
        self.api_url_payLink = self.api_url + 'payLink'
        self.api_url_transfer = self.api_url + 'transfer'
        self.api_url_transaction_history = self.api_url + 'TransactionHistory'

    def sign(self, data):
        data['id'] = self.id
        data = {k: v for k, v in data.items() if v is not None}  # 过滤掉 None 值
        sorted_data = dict(sorted(data.items()))
        sorted_data['sign'] = self._generate_signature(sorted_data)
        return sorted_data

    def _generate_signature(self, data):
        query_string = urlencode(data) + '&token=' + self.token
        return hashlib.md5(query_string.encode('utf-8')).hexdigest().upper()

    def post(self, data):
        signed_data = self.sign(data)
        try:
            print('请求地址', self.url)
            print('请求数据', signed_data)
            response = requests.post(self.url, data=signed_data)
            response.raise_for_status()
            return response.json()  # 尝试解析 JSON
        except requests.exceptions.RequestException as e:
            print(f"请求失败: {e}")
            return {"error": str(e)}  # 返回错误信息
        except ValueError as e:
            print(f"解析 JSON 失败: {e}")
            return {"error": "无效的 JSON 响应"}

    def payLink(self, unique_id=None, name=None, amount=None, return_url=None, coin='USDT'):
        self.url = self.api_url_payLink
        data = {
            'unique_id': unique_id,
            'name': name,
            'amount': amount,
            'return_url': return_url,
            'coin': coin
        }
        return self.post(data)

    def transfer(self, unique_id=None, name=None, amount=None, to_user_id=None, coin='USDT'):
        self.url = self.api_url_transfer
        data = {
            'unique_id': unique_id,
            'name': name,
            'amount': amount,
            'to_user_id': to_user_id,
            'coin': coin
        }
        return self.post(data)

    def shop_transaction_history(self, unique_id=None, name=None, amount=None, to_user_id=None, coin='USDT'):
        self.url = self.api_url_transaction_history
        data = {
            'unique_id': unique_id,
            'name': name,
            'amount': amount,
            'to_user_id': to_user_id,
            'coin': coin
        }
        return self.post(data)

    def notify(self, data):
        if self.check_sign(data):
            if data.get('status') == 'success' and data.get('code') == 10000:
                # 数据正常
                return True
            else:
                # 数据不正常
                return False
        else:
            return False

    def check_sign(self, data):
        in_sign = data['sign']
        del data['sign']
        data = {k: v for k, v in data.items() if v is not None}  # 过滤掉 None 值
        sorted_data = dict(sorted(data.items()))
        sign = self._generate_signature(sorted_data)
        return in_sign == sign

if __name__ == '__main__':
    okpay = OkayPay()
    pay_link_response = okpay.payLink(
        name='充值',  # 显示信息
        amount=10,  # 充值金额
        coin='CNY'  # 货币类型
    )

    if 'data' in pay_link_response and 'pay_url' in pay_link_response['data']:
        pay_url = pay_link_response['data']['pay_url']
        print(pay_url)
    else:
        print("无法获取支付链接")
