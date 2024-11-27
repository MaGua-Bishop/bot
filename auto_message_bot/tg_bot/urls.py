from django.urls import path
from .views import BotView

app_name = 'tg_bot'

urlpatterns = [
    path('auto_message_bot', BotView.as_view()),
]
