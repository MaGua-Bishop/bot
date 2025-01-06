from django.urls import path
from .views import BotView,RechargeView

app_name = 'tg_bot'

urlpatterns = [
    path('lottery_bot_cn', BotView.as_view()),
    path('lottery_bot_cn/recharge', RechargeView.as_view()),
]
