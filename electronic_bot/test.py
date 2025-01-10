import random
from decimal import Decimal, ROUND_DOWN


def divide_red_packet(total_amount, count):
    """
    将总金额 total_amount 分为 count 个随机红包金额。

    :param total_amount: Decimal, 总金额
    :param count: int, 红包个数
    :return: List[Decimal], 每个红包的金额列表
    """
    if count <= 0:
        raise ValueError("红包个数必须大于 0")
    if total_amount <= 0:
        raise ValueError("总金额必须大于 0")

    # 将总金额转换为分（避免浮点数精度问题）
    total_cents = int((total_amount * 100).to_integral_value(rounding=ROUND_DOWN))
    if count > total_cents:
        raise ValueError("红包个数不能大于总金额的分单位数")

    # 随机生成 count 个红包金额的分
    red_packets = []
    for _ in range(count):
        # 确保每个红包至少 1 分，剩余金额分布
        remaining_cents = total_cents - len(red_packets) - (count - len(red_packets) - 1)
        max_cents = min(remaining_cents, (total_cents // count) * 2)
        amount = random.randint(1, max_cents)
        red_packets.append(amount)
        total_cents -= amount

    # 转换为 Decimal 类型并四舍五入到两位小数
    red_packets = [Decimal(amount) / 100 for amount in red_packets]

    # 调整误差以确保总和等于 total_amount
    difference = total_amount - sum(red_packets)
    if difference != 0:
        red_packets[0] += difference

    return red_packets


def divide_red_packet(total_amount, count):
    """
    将总金额 total_amount 分为 count 个相对平均的随机红包金额。

    :param total_amount: Decimal, 总金额
    :param count: int, 红包个数
    :return: List[Decimal], 每个红包的金额列表
    """
    if count <= 0:
        raise ValueError("红包个数必须大于 0")
    if total_amount <= 0:
        raise ValueError("总金额必须大于 0")

    # 将总金额转换为分（避免浮点数精度问题）
    total_cents = int((total_amount * 100).to_integral_value(rounding=ROUND_DOWN))
    if count > total_cents:
        raise ValueError("红包个数不能大于总金额的分单位数")

    # 平均金额
    avg_cents = total_cents // count
    # 余数
    remainder = total_cents % count

    # 初始化红包列表，每个红包先分配平均金额
    red_packets = [avg_cents for _ in range(count)]

    # 随机分配余数，使红包金额更加随机
    for _ in range(remainder):
        idx = random.randint(0, count - 1)
        red_packets[idx] += 1

    # 转换为 Decimal 类型并四舍五入到两位小数
    red_packets = [Decimal(amount) / 100 for amount in red_packets]

    return red_packets


def divide_red_packets(total_amount, count):
    """
    将总金额 total_amount 分为 count 个随机且金额差距适中的红包。

    :param total_amount: Decimal, 总金额
    :param count: int, 红包个数
    :return: List[Decimal], 每个红包的金额列表
    """
    if count <= 0:
        raise ValueError("红包个数必须大于 0")
    if total_amount <= 0:
        raise ValueError("总金额必须大于 0")

    # 将总金额转换为分（避免浮点数精度问题）
    total_cents = int((total_amount * 100).to_integral_value(rounding=ROUND_DOWN))
    if count > total_cents:
        raise ValueError("红包个数不能大于总金额的分单位数")

    # 初始化红包列表
    red_packets = []
    for i in range(count):
        # 确保每个红包的金额在合理范围内，例如 0.5x 到 1.5x 的平均值
        min_cents = max(1, int(0.5 * (total_cents / (count - i))))
        max_cents = min(total_cents - (count - i - 1), int(1.5 * (total_cents / (count - i))))
        amount = random.randint(min_cents, max_cents)
        red_packets.append(amount)
        total_cents -= amount

    # 转换为 Decimal 类型并四舍五入到两位小数
    red_packets = [Decimal(amount) / 100 for amount in red_packets]

    # 调整误差以确保总和等于 total_amount
    difference = total_amount - sum(red_packets)
    if difference != 0:
        red_packets[0] += difference

    return red_packets

# 示例调用
if __name__ == '__main__':
    total_amount = Decimal('200.00')  # 总金额 100
    count = 10000  # 红包数量

    amounts = divide_red_packets(total_amount, count)
    print(amounts)

    print(sum(amounts))
    print(len(amounts))
