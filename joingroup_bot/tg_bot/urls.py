from django.urls import path
from .views import BotView, RechargeView

app_name = 'tg_bot'

urlpatterns = [
    path('joingroup_bot/', BotView.as_view()),
    path('joingroup_bot/recharge/', RechargeView.as_view()),
]
