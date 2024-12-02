from django.urls import path
from . import views

app_name = 'agentadmin'

urlpatterns = [
    path('login', views.login_view, name='login'),
    path('kaijiangxinxi', views.kaijiangxinxi, name='kaijiangxinxi'),
    path('zhanghaoguanli', views.zhanghaoguanli, name='zhanghaoguanli'),
    path('agentadmin_info', views.agentadmin_info, name='agentadmin_info'),  # 账号管理 获取代理管理员信息
    path('kaiguanpan', views.kaiguanpan, name='kaiguanpan'),  # 账号管理 开|关盘
    path('xiugaimima', views.xiugaimima, name='xiugaimima'),
    path('tongjifenxi', views.tongjifenxi, name='tongjifenxi'),
    path('tongjifenxi_info', views.tongjifenxi_info, name='tongjifenxi_info'),  # 选择日期查看统计分析
    path('sxfen', views.sxfen, name='sxfen'),  # 账号管理的上下分按钮
    path('xiafen', views.xiafen, name='xiafen'),  # 上下分按钮中的上分
    path('shangfen', views.shangfen, name='shangfen'),  # 上下分按钮中的下分
    path('xiugaixiane', views.xiugaixiane, name='xiugaixiane'),  # 账号管理的查看和修改限额
    path('chauser', views.chauser, name='chauser'),  # 账号管理的查看会员余额
    path('weijiesuan', views.weijiesuan, name='weijiesuan'),
    path('lishi', views.lishi, name='lishi'),
    path('anquantuichu', views.anquantuichu, name='anquantuichu'),
]
