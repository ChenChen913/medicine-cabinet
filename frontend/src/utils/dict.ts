// 后端出参为英文枚举名（见 types/index.ts 的 DosageForm），此处做中文展示映射。
// 键必须是英文枚举名；中文文案与 MedicineList 表单下拉的 label 保持一致。
export const DOSAGE_FORM_TEXT: Record<string, string> = {
  TABLET: '片剂',
  CAPSULE: '胶囊',
  GRANULE: '颗粒',
  LIQUID: '液体',
  EXTERNAL: '外用',
  INJECTION: '注射剂',
  OTHER: '其他'
}

// 未知枚举值原样展示，空值显示占位符，避免页面出现空白单元格
export const getDosageFormText = (dosageForm?: string): string => {
  if (!dosageForm) return '-'
  return DOSAGE_FORM_TEXT[dosageForm] || dosageForm
}
