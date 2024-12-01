from django.urls import path
from . import views

app_name = 'agentadmin'

urlpatterns = [
    path('login', views.login_view, name='login'),
    path('kaijiangxinxi', views.kaijiangxinxi, name='kaijiangxinxi'),
    path('zhanghaoguanli', views.zhanghaoguanli, name='zhanghaoguanli'),
    path('agentadmin_info', views.agentadmin_info, name='agentadmin_info'), # 账号管理 获取代理管理员信息
    path('kaiguanpan', views.kaiguanpan, name='kaiguanpan'),# 账号管理 开|关盘
    path('xiugaimima', views.xiugaimima, name='xiugaimima'),
    path('tongjifenxi', views.tongjifenxi, name='tongjifenxi'),
    path('weijiesuan', views.weijiesuan, name='weijiesuan'),
    path('lishi', views.lishi, name='lishi'),
    path('anquantuichu', views.anquantuichu, name='anquantuichu'),
]

