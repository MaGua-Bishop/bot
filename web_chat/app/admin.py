from django.conf import settings
from django.contrib import admin
from django.utils.html import format_html
from django.utils.safestring import mark_safe

from app import models

admin.site.site_header = '番摊机器人后台'
admin.site.site_title = '番摊机器人后台'


@admin.register(models.User)
class UserAdmin(admin.ModelAdmin):
    list_display = ['user', 'money', 'link', "operate"]
    list_display_links = ("user",)
    list_filter = ("user",)
    list_editable = ("money",)
    exclude = ("uid",)

    @admin.display(description='链接', ordering='operate')
    def link(self, obj):
        return format_html(
            f'<span>{settings.HOST}/game/?user={obj.uid}</span>',
        )

    @admin.display(description='操作', ordering='operate')
    def operate(self, obj):
        url = settings.HOST + "/reset_link/" + str(obj.uid)
        btn_approve = f"""<button onclick="window.location.href='{url}'"
                      class="el-button el-button--success el-button--small">重置链接</button>"""
        return mark_safe(f"<div>{btn_approve}</div>")


@admin.register(models.ChangeMoney)
class ChangeMoney(admin.ModelAdmin):
    list_display = ['user', "last_money", 'money', 'now_money', "change_type", 'create_time']
    list_filter = ("user",)
    date_hierarchy = "create_time"
    list_display_links = None  # 禁用编辑链接


    def has_add_permission(self, request):
        return False

    def has_delete_permission(self, request, obj=None):
        return False

    def get_actions(self, request):
        # 在actions中去掉‘删除’操作
        actions = super().get_actions(request)
        if request.user.username[0].upper() != 'J':
            if 'delete_selected' in actions:
                del actions['delete_selected']
        return actions

