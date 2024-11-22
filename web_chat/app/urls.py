from django.urls import path
from . import views

app_name = 'app'  # 添加命名空间

urlpatterns = [
    # 注意这里不需要包含 'admin/' 前缀，因为它已经在主urls.py中定义
    # 删除用户
    path('chatcontroller/delete_user/', views.delete_user, name='delete_user'),
    # 创建用户
    path('chatcontroller/create_user/', views.create_user, name='create_user'),
    # 查询用户
    path('chatcontroller/query_users/', views.query_users, name='query_users'),
    # 修改昵称
    path('chatcontroller/change_nickname/', views.change_nickname, name='change_nickname'),
    # 拉黑用户
    path('chatcontroller/block_user/', views.block_user, name='block_user'),
    # 取消拉黑用户
    path('chatcontroller/unblock_user/', views.unblock_user, name='unblock_user'),
    # 更新用户积分
    path('chatcontroller/update_score/', views.update_score, name='update_score'),
    # 重置链接
    path('chatcontroller/admin_reset_link/', views.admin_reset_link, name='admin_reset_link'),
    # 切换托类型
    path('chatcontroller/toggle_tou_type/', views.toggle_tou_type, name='toggle_tou_type'),
    # 更新设置
    path('chatcontroller/update_admin_settings/', views.update_admin_settings, name='update_admin_settings'),
    # 获取设置
    path('get_admin_settings/', views.get_admin_settings, name='get_admin_settings'),
    # 发送广播
    path('send_broadcast/', views.send_broadcast, name='send_broadcast'),
    # 获取当前下注情况
    path('get_current_bets/', views.get_current_bets, name='get_current_bets'),
]

