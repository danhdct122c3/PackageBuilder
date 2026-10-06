# Sinh lại các file đáp án trong thư mục này từ bản Python tham chiếu của SV3.
# Chạy trong thư mục SV3_D6_config/D6 (cần score.py v1.1 ở ../../SV3_D4_config/D4):
#     python gen_reference.py <thư_mục_ra>
# Chỉ chạy lại khi SV3 đổi prompt/schema/từ khóa/matching_rules hoặc seed, rồi chép file mới vào đây.
import json, sys, copy, os
sys.path.insert(0, '.'); sys.path.insert(0, '../../SV3_D4_config/D4')
import testset, fallback_extract as fb, gemini_extract as g, score, form_extract as fe
OUT = (sys.argv[1] if len(sys.argv) > 1 else '.') + os.sep
tests = testset.TESTS + testset.TESTS_HOLDOUT
# 1. fallback
fbc = []
for tid, text, exp in tests:
    d = fb.extract(text)
    fbc.append(dict(id=tid, text=text, result={k: d[k] for k in ('objective_codes','environment_codes','industry_codes','topic_codes','expected_output_codes','available_input_codes','area_value','area_unit')}))
form = json.load(open('form_example.json', encoding='utf-8'))
out, ai_text, kw_text, sent, unknown = fe.split_form(form)
d = fb.extract(kw_text)
fbc.append(dict(id='FORM_EXAMPLE', text=kw_text, result={k: d[k] for k in ('objective_codes','environment_codes','industry_codes','topic_codes','expected_output_codes','available_input_codes','area_value','area_unit')}))
json.dump(fbc, open(OUT+'fallback_cases.json','w',encoding='utf-8'), ensure_ascii=False, indent=1)
# form split
forms = [form, dict(form, objectiveRaw='', expectedOutputs='Bản đồ sinh khối\nBáo cáo MRV', areaValue='2.000 ha', providedInputsRaw=None),
         dict(customerName='A', contactEmail='a@b.c', objectiveRaw='', projectName='', locationDescription='', companyName=''),
         dict(form, areaValue='1,5 km2'), dict(form, areaValue=350), dict(form, areaValue='khoảng vài trăm')]
fc = []
for f in forms:
    out, ai_text, kw_text, sent, unknown = fe.split_form(f)
    av, au, aw = fe.parse_area(f.get('areaValue'))
    fc.append(dict(form=f, ai_text=ai_text, kw_text=kw_text, sent=sent, area_value=av, area_unit=au, area_warning=aw,
                   user_input=fe.FORM_INSTRUCTION + ai_text + "\n\nTrả về đúng một object JSON, không thêm chữ nào ngoài JSON."))
json.dump(fc, open(OUT+'form_cases.json','w',encoding='utf-8'), ensure_ascii=False, indent=1)
# 2. postcheck
full, gem = g.load_schemas(); codes = g.allowed_codes(full)
raws = [
 {"objective_raw":"x","objective_codes":["CARBON_STOCK_ASSESSMENT","CARBON_STOCK_ASSESSMENT","CARBON_CREDIT_MRV"],"environment_codes":["FOREST","JUNGLE"],"industry_codes":["FORESTRY","FORESTRY","FORESTRY"],"topic_codes":["carbon","Carbon"],"expected_output_codes":["MRV_REPORT","FAKE_OUT","MRV_REPORT"],"available_input_codes":["AOI_BOUNDARY","MRV_REPORT"],"confidence":{"objective":0.9,"overall":0.85}},
 {"objective_raw":None,"objective_codes":[],"environment_codes":[],"industry_codes":["CIVIL_ENGINEERING"],"topic_codes":[],"expected_output_codes":[],"available_input_codes":[],"confidence":{"objective":0,"overall":0.2},"notes":"Nhu cầu ngoài phạm vi dịch vụ hiện có: khảo sát đê"},
 {"objective_raw":"y","objective_codes":["GHG_EMISSION_MEASUREMENT"],"environment_codes":["RICE_FIELD","CROPLAND","RICE_FIELD"],"confidence":{"objective":0.8,"overall":0.6}},
]
pc = []
for r in raws:
    data, w = g.postcheck(copy.deepcopy(r), full, codes)
    pc.append(dict(raw=r, result={k: data.get(k) for k in codes}, warnings=[x for x in w if not x.startswith('Sai schema')]))
json.dump(pc, open(OUT+'postcheck_cases.json','w',encoding='utf-8'), ensure_ascii=False, indent=1)
# 3. scoring
reqs = []
for c in fbc: reqs.append(('FB_'+c['id'], c['result']))
for fn in ('gemini_eval.json','gemini_eval_holdout.json'):
    for r in json.load(open(fn, encoding='utf-8'))['rows']:
        reqs.append(('GM_'+r['id'], {f+'_codes': r[f]['returned'] for f in ('objective','environment','industry','expected_output')}))
SCEN={'VD1':dict(objective=['CARBON_STOCK_ASSESSMENT','CARBON_CREDIT_MRV'],environment=['FOREST'],industry=['FORESTRY'],expected_output=['AGB_CARBON_STOCK_MAP','MRV_REPORT'],topic=['carbon']),
 'VD2':dict(objective=['GHG_EMISSION_MEASUREMENT'],environment=['RICE_FIELD'],industry=['AGRICULTURE'],expected_output=['SEASONAL_CH4_EMISSION'],topic=['methane']),
 'VD3_EMPTY':dict(objective=['FIRE_RISK_PREVENTION'],environment=['RICE_FIELD','BUILDING'],topic=['lidar','methane'])}
for k,v in SCEN.items(): reqs.append((k,v))
sc = []
for rid, req in reqs:
    rq = score.normalize(req)
    rows, keep = score.score(req)
    sc.append(dict(id=rid, requirement=rq,
        rows=[{k: r[k] for k in ('service_code','match_score','objective_score','use_case_score','industry_score','output_score','tag_score','reason')} for r in rows],
        shortlist=[r['service_code'] for r in keep]))
json.dump(sc, open(OUT+'score_cases.json','w',encoding='utf-8'), ensure_ascii=False, indent=1)
print(len(fbc), len(fc), len(pc), len(sc))
