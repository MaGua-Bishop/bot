from django.contrib import admin
from django.utils.safestring import mark_safe
from django.urls import reverse
from admin_web.models import Audit


@admin.register(Audit)
class Audit_Manager(admin.ModelAdmin):
    list_display = ['id', 'account', 'phone', 'type', 'status', 'create_time', 'operate']
    ordering = ['status']
    search_fields = ['account', 'phone']  # 顶部搜索框
    # list_filter = ['account', 'phone']    # 侧边筛选器

    @admin.display(description='操作', ordering='operate')
    def operate(self, obj):
        if obj.status == 0:
            approve_url = reverse('audit_approve', args=[obj.pk])
            btn_approve = f"""<button onclick="window.location.href='{approve_url}'"
                          class="el-button el-button--success el-button--small">通过</button>"""
            reject_url = reverse('reject_audit', args=[obj.pk])
            btn_reject = f"""<button onclick="window.location.href='{reject_url}'"
                          class="el-button el-button--danger el-button--small">拒绝</button>"""
            return mark_safe(f"<div>{btn_approve} {btn_reject}</div>")
        elif obj.status == 1:
            btn_approve = f"""<button disabled
                          class="el-button el-button--success el-button--small ">已通过</button>"""
            return mark_safe(f"<div>{btn_approve} </div>")
        elif obj.status == 2:
            btn_reject = f"""<button disabled
                          class="el-button el-button--danger el-button--small ">已拒绝</button>"""
            return mark_safe(f"<div>{btn_reject} </div>")
