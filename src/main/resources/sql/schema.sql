-- 家庭智慧药箱管理系统 - 数据库建表脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS medicine_cabinet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE medicine_cabinet;

-- 1. 药品主表
CREATE TABLE IF NOT EXISTS medicines (
  -- 主键
  id VARCHAR(36) PRIMARY KEY COMMENT '药品ID',
  
  -- 基础信息
  name VARCHAR(255) NOT NULL COMMENT '药品名称',
  image_url VARCHAR(500) COMMENT '药品图片URL',
  dosage_form ENUM('TABLET', 'CAPSULE', 'GRANULE', 'LIQUID', 'EXTERNAL', 'INJECTION', 'OTHER') NOT NULL COMMENT '剂型',
  storage_location VARCHAR(255) COMMENT '存放位置',
  
  -- 库存相关
  total_quantity DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '当前剩余总量',
  unit VARCHAR(20) NOT NULL COMMENT '单位（粒/ml/包/片等）',
  threshold DECIMAL(10, 2) NOT NULL DEFAULT 10 COMMENT '预警阈值',
  
  -- 时间相关
  production_date DATE COMMENT '生产日期',
  expiry_date DATE NOT NULL COMMENT '过期日期',
  last_purchase_date DATE COMMENT '最近购买日期',
  
  -- 医疗说明
  symptoms_treated TEXT COMMENT '适应症',
  dosage_instruction VARCHAR(500) COMMENT '服用说明',
  side_effects TEXT COMMENT '副作用（详情页显示）',
  
  -- 算法字段
  usage_frequency_score INT NOT NULL DEFAULT 0 COMMENT '使用频率分数',
  
  -- 状态
  status ENUM('NORMAL', 'LOW_STOCK', 'EXPIRED', 'DEPLETED') NOT NULL DEFAULT 'NORMAL' COMMENT '状态',
  
  -- 审计字段
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  INDEX idx_expiry_date (expiry_date),
  INDEX idx_status (status),
  INDEX idx_usage_frequency (usage_frequency_score DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品主表';

-- 2. 类别表
CREATE TABLE IF NOT EXISTS categories (
  id VARCHAR(36) PRIMARY KEY COMMENT '类别ID',
  name VARCHAR(100) NOT NULL UNIQUE COMMENT '类别名称',
  description TEXT COMMENT '类别描述',
  icon VARCHAR(100) COMMENT '图标',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='类别表';

-- 3. 药品-类别关联表
CREATE TABLE IF NOT EXISTS medicine_categories (
  medicine_id VARCHAR(36) NOT NULL COMMENT '药品ID',
  category_id VARCHAR(36) NOT NULL COMMENT '类别ID',
  PRIMARY KEY (medicine_id, category_id),
  FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE,
  FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品-类别关联表';

-- 4. 用药记录表
CREATE TABLE IF NOT EXISTS usage_logs (
  id VARCHAR(36) PRIMARY KEY COMMENT '记录ID',
  medicine_id VARCHAR(36) NOT NULL COMMENT '药品ID',
  usage_time TIMESTAMP NOT NULL COMMENT '服用时间',
  amount DECIMAL(10, 2) NOT NULL COMMENT '服用量',
  user_name VARCHAR(100) NOT NULL COMMENT '服用人',
  symptom VARCHAR(255) COMMENT '对应症状',
  notes TEXT COMMENT '备注',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  
  FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE,
  INDEX idx_medicine_id (medicine_id),
  INDEX idx_usage_time (usage_time DESC),
  INDEX idx_user_name (user_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用药记录表';

-- 5. 购物清单表
CREATE TABLE IF NOT EXISTS shopping_list (
  id VARCHAR(36) PRIMARY KEY COMMENT '清单项ID',
  medicine_id VARCHAR(36) NOT NULL COMMENT '药品ID',
  reason ENUM('EXPIRED', 'DEPLETED', 'MANUAL') NOT NULL COMMENT '添加原因',
  custom_note TEXT COMMENT '自定义备注',
  status ENUM('PENDING', 'PURCHASED') NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  purchased_at TIMESTAMP NULL COMMENT '购买时间',
  
  FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE,
  INDEX idx_status (status),
  INDEX idx_added_at (added_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物清单表';

-- 插入初始类别数据
INSERT INTO categories (id, name, description, icon) VALUES
('cat-001', '感冒药', '用于治疗感冒、发烧等症状', 'medicine'),
('cat-002', '止痛药', '用于缓解各种疼痛', 'heart'),
('cat-003', '肠胃药', '用于治疗消化系统疾病', 'stomach'),
('cat-004', '外用药', '外用药品，如创可贴、碘伏等', 'band-aid'),
('cat-005', '维生素', '各类维生素补充剂', 'vitamin');
