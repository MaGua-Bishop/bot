from django.contrib import admin
from django.utils.safestring import mark_safe
from django.urls import reverse
from admin_web.models import Audit
from import_export.admin import ExportActionModelAdmin
from import_export.formats.base_formats import XLSX
from import_export import resources, fields


class AuditResource(resources.ModelResource):
    account = fields.Field(attribute='account', column_name='账号')
    phone = fields.Field(attribute='phone', column_name='电话')
    type_display = fields.Field(column_name='类型')
    status_display = fields.Field(column_name='审核状态')
    create_time_display = fields.Field(column_name='申请时间')

    class Meta:
        model = Audit
        fields = ['account', 'phone', 'type_display', 'status_display', 'create_time_display']
        export_order = ['account', 'phone', 'type_display', 'status_display', 'create_time_display']

    def dehydrate_type_display(self, audit):
        type_mapping = {
            0: "办理存款彩金",
            1: "办理提现彩金",
        }
        return type_mapping.get(audit.type, "未知类型")

    def dehydrate_status_display(self, audit):
        status_mapping = {
            0: "待处理",
            1: "已通过",
            2: "已拒绝",
        }
        return status_mapping.get(audit.status, "未知状态")

    def dehydrate_create_time_display(self, audit):
        if audit.create_time:
            return audit.create_time.replace(tzinfo=None).strftime("%Y-%m-%d %H:%M:%S")
        return ""


@admin.register(Audit)
class Audit_Manager(ExportActionModelAdmin):
    list_display = ['id', 'account', 'phone', 'type', 'status', 'create_time', 'operate']
    ordering = ['status']
    search_fields = ['account', 'phone']
    list_filter = ['status']
    resource_class = AuditResource

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

    def get_export_formats(self):
        """仅允许导出为Excel格式"""
        return [XLSX]
