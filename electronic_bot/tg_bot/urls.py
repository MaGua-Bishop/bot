from django.urls import path
from .views import BotView,RechargeView

app_name = 'tg_bot'

urlpatterns = [
    path('electronic_bot', BotView.as_view()),
    path('electronic_bot/recharge', RechargeView.as_view()),
]
