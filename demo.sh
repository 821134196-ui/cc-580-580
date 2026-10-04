#!/usr/bin/env bash
# ============================================================
# 端到端演示脚本：验证编号唯一、并发签发、更正后旧证失效、
# 补考后资格重新核验。需先通过 ./start.sh 启动系统。
# ============================================================
set -euo pipefail
BASE="${1:-http://localhost:8080}"
J() { python3 -c "import sys,json;d=json.load(sys.stdin);print(eval(sys.argv[1]))" "$2" <<<"$1" 2>/dev/null || echo "?"; }

echo "========================================================"
echo " 场景 1：并发签发 —— 8 个请求同时抢发同一学员的证书"
echo "========================================================"
echo "（张三 / JAVA-BASIC，事前状态：学时 36/32 达标，考试 88 分通过）"
for i in $(seq 1 8); do
  curl -s -o /tmp/issue_$i.json -w "%{http_code}" -X POST "$BASE/api/certificates/issue" \
    -H 'Content-Type: application/json' \
    -d '{"studentId":1,"courseId":1,"confirmedName":"张三","issuedBy":"教务-并发测试"}' &
done
wait
ok=0; conflict=0
for i in $(seq 1 8); do
  code=$(cat /tmp/issue_$i.json | python3 -c "import sys,json;print(json.load(sys.stdin).get('certNumber',''))" 2>/dev/null || true)
  if [ -n "$code" ]; then ok=$((ok+1)); echo "  请求 $i: 签发成功 -> $code"; else conflict=$((conflict+1)); fi
done
echo "结果: 成功 $ok 笔，被拒绝 $conflict 笔（预期 1 成功 + 7 拒绝）"
echo
echo "数据库中该学员该课程的有效证书数:"
curl -s "$BASE/api/certificates?studentId=1" | python3 -c "
import sys, json
certs = json.load(sys.stdin)
active = [c for c in certs if c['courseId'] == 1 and c['status'] == 'ACTIVE']
numbers = [c['certNumber'] for c in certs]
print(f\"  有效证书: {len(active)} 张 -> {[c['certNumber'] for c in active]}\")
print(f\"  全部证书编号: {sorted(set(numbers))} (共 {len(numbers)} 张, 去重 {len(set(numbers))} 个编号)\")
assert len(active) == 1, '有效证书必须恰好 1 张'
assert len(numbers) == len(set(numbers)), '编号不得重复'
print('  ✔ 编号唯一、有效证书唯一')
"

echo
echo "========================================================"
echo " 场景 2：补考后资格重新核验（不使用过期成绩快照）"
echo "========================================================"
echo "（李四 / JAVA-BASIC：首考 45 分未通过）"
echo "-- 补考前资格判定:"
curl -s "$BASE/api/eligibility?studentId=2&courseId=1" | python3 -m json.tool --no-ensure-ascii | grep -E 'eligible|latestScore|reasons' || true
echo "-- 直接签发（应被拒绝）:"
curl -s -X POST "$BASE/api/certificates/issue" -H 'Content-Type: application/json' \
  -d '{"studentId":2,"courseId":1,"confirmedName":"李四"}' | python3 -m json.tool --no-ensure-ascii
echo "-- 登记补考成绩 78 分:"
curl -s -X POST "$BASE/api/records/exam" -H 'Content-Type: application/json' \
  -d '{"studentId":2,"courseId":1,"score":78}' | python3 -m json.tool --no-ensure-ascii
echo "-- 补考后资格判定（实时重算）:"
curl -s "$BASE/api/eligibility?studentId=2&courseId=1" | python3 -m json.tool --no-ensure-ascii | grep -E 'eligible|latestScore|attemptCount' || true
echo "-- 再次签发（应成功）:"
curl -s -X POST "$BASE/api/certificates/issue" -H 'Content-Type: application/json' \
  -d '{"studentId":2,"courseId":1,"confirmedName":"李四","issuedBy":"教务-演示"}' | python3 -m json.tool --no-ensure-ascii | grep -E 'certNumber|status|holderName' || true

echo
echo "========================================================"
echo " 场景 3：姓名更正 —— 退回补件 → 补件 → 通过换发 → 旧证失效"
echo "========================================================"
cert_json=$(curl -s "$BASE/api/certificates?studentId=2")
cert_id=$(J "$cert_json" "d[0]['id']")
old_number=$(J "$cert_json" "d[0]['certNumber']")
echo "-- 对证书 $old_number (id=$cert_id) 提交更正申请（材料不足）:"
corr=$(curl -s -X POST "$BASE/api/corrections" -H 'Content-Type: application/json' \
  -d "{\"certificateId\":$cert_id,\"requestedName\":\"李泗\",\"reason\":\"同音错字，与身份证不符\",\"materials\":[]}")
echo "$corr" | python3 -m json.tool --no-ensure-ascii | grep -E 'status|requestedName' || true
corr_id=$(J "$corr" "d['id']")
echo "-- 教务退回补件:"
curl -s -X POST "$BASE/api/corrections/$corr_id/return" -H 'Content-Type: application/json' \
  -d '{"note":"请补充身份证扫描件","reviewedBy":"教务-演示"}' | python3 -m json.tool --no-ensure-ascii | grep -E 'status|reviewNote' || true
echo "-- 学员补件重提:"
curl -s -X POST "$BASE/api/corrections/$corr_id/resubmit" -H 'Content-Type: application/json' \
  -d "{\"certificateId\":$cert_id,\"requestedName\":\"李泗\",\"reason\":\"同音错字，与身份证不符\",\"materials\":[\"身份证扫描件\"]}" \
  | python3 -m json.tool --no-ensure-ascii | grep -E 'status|submitCount' || true
echo "-- 教务审核通过（作废旧证 + 签发新证）:"
approved=$(curl -s -X POST "$BASE/api/corrections/$corr_id/approve" -H 'Content-Type: application/json' \
  -d '{"reviewedBy":"教务-演示"}')
echo "$approved" | python3 -m json.tool --no-ensure-ascii | grep -E 'status|newCertNumber' || true
new_number=$(J "$approved" "d['newCertNumber']")
echo "-- 核验旧证 $old_number（应已作废）:"
curl -s "$BASE/api/verify/$old_number" | python3 -m json.tool --no-ensure-ascii | grep -E 'status|revokeReason|replacedByNumber' || true
echo "-- 核验新证 $new_number（应有效，V2）:"
curl -s "$BASE/api/verify/$new_number" | python3 -m json.tool --no-ensure-ascii | grep -E 'status|holderName|version' || true
echo
echo "全部演示场景执行完毕。"
