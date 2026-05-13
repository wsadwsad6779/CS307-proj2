如果复制多行代码，多按一次换行，有时候最新一条代码需要多按一下才能执行。
# 一、基础必测流程 SQL

## 0. 环境检查与异常检查

这一部分主要测试：程序遇到错误时不能崩溃，要有日志或错误提示。

### 输入

```sql
show tables;

describe not_exist_table;

drop table not_exist_table;

select * from not_exist_table;
```

程序继续运行，不退出。


# 二、建表与 DDL 检查

## 1. 创建主测试表

### 输入

```sql
create table t(  id int,  name varchar,  age int,  gpa double);
```

---

## 2. 重复建表检查

### 输入

```sql
create table t(  id int,  name varchar,  age int,  gpa double);
```

### 预期输出

```text
ERROR: table t already exists
程序继续运行，不退出。
```

---

## 3. 查看所有表

### 输入

```sql
show tables;
```

### 预期输出

```text
|-----------|
| Tables    |
|-----------|
| t         |
|-----------|
```

如果后面创建了其他表，例如 `course`，这里也应该显示出来。

---

## 4. 查看表结构

### 输入

```sql
describe t;
```

### 预期输出

```text
| Field | Type    |
| id    | int     |
| name  | varchar |
| age   | int     |
| gpa   | double  |
```


# 三、插入 36 条测试数据

PDF 要求展示时至少一张表超过 30 行数据，所以这里准备 36 行。

## 输入

```sql
insert into t (id, name, age, gpa) values (1, 'alice', 18, 3.60);
insert into t (id, name, age, gpa) values (2, 'bob', 19, 3.65);
insert into t (id, name, age, gpa) values (3, 'cathy', 18, 3.86);
insert into t (id, name, age, gpa) values (4, 'david', 20, 2.34);
insert into t (id, name, age, gpa) values (5, 'eva', 21, 3.25);
insert into t (id, name, age, gpa) values (6, 'frank', 22, 3.20);
insert into t (id, name, age, gpa) values (7, 'grace', 19, 3.90);
insert into t (id, name, age, gpa) values (8, 'henry', 20, 2.80);
insert into t (id, name, age, gpa) values (9, 'ivy', 18, 3.10);
insert into t (id, name, age, gpa) values (10, 'jack', 23, 3.75);
insert into t (id, name, age, gpa) values (11, 'kate', 21, 2.95);
insert into t (id, name, age, gpa) values (12, 'leo', 19, 3.40);
insert into t (id, name, age, gpa) values (13, 'mia', 22, 3.88);
insert into t (id, name, age, gpa) values (14, 'nick', 20, 3.00);
insert into t (id, name, age, gpa) values (15, 'olivia', 24, 3.92);
insert into t (id, name, age, gpa) values (16, 'peter', 18, 2.50);
insert into t (id, name, age, gpa) values (17, 'queen', 21, 3.55);
insert into t (id, name, age, gpa) values (18, 'ryan', 23, 3.15);
insert into t (id, name, age, gpa) values (19, 'sophia', 22, 2.70);
insert into t (id, name, age, gpa) values (20, 'tom', 20, 3.35);
insert into t (id, name, age, gpa) values (21, 'uma', 19, 3.05);
insert into t (id, name, age, gpa) values (22, 'victor', 24, 3.80);
insert into t (id, name, age, gpa) values (23, 'wendy', 18, 3.45);
insert into t (id, name, age, gpa) values (24, 'xavier', 25, 2.90);
insert into t (id, name, age, gpa) values (25, 'yuki', 21, 3.70);
insert into t (id, name, age, gpa) values (26, 'zoe', 20, 3.99);
insert into t (id, name, age, gpa) values (27, 'anna', 23, 2.60);
insert into t (id, name, age, gpa) values (28, 'brian', 22, 3.33);
insert into t (id, name, age, gpa) values (29, 'clara', 19, 3.58);
insert into t (id, name, age, gpa) values (30, 'daniel', 18, 3.22);
insert into t (id, name, age, gpa) values (31, 'eric', 26, 3.11);
insert into t (id, name, age, gpa) values (32, 'fiona', 25, 3.77);
insert into t (id, name, age, gpa) values (33, 'george', 24, 2.88);
insert into t (id, name, age, gpa) values (34, 'helen', 23, 3.66);
insert into t (id, name, age, gpa) values (35, 'iris', 22, 3.44);
insert into t (id, name, age, gpa) values (36, 'john', 21, 3.01);
```

### 预期输出

每条 insert 都应该成功：

```text
OK: 1 row inserted
```

总共应成功插入 36 行。


# 四、基础 SELECT 与 SeqScan 检查

## 1. 全表扫描

### 输入

```sql
select * from t;
```

### 预期输出

应该显示 36 行，字段为：

```text
id | name | age | gpa
```

完整数据应包含：

```text
1  | alice  | 18 | 3.60
2  | bob    | 19 | 3.65
3  | cathy  | 18 | 3.86
4  | david  | 20 | 2.34
5  | eva    | 21 | 3.25
6  | frank  | 22 | 3.20
7  | grace  | 19 | 3.90
8  | henry  | 20 | 2.80
9  | ivy    | 18 | 3.10
10 | jack   | 23 | 3.75
11 | kate   | 21 | 2.95
12 | leo    | 19 | 3.40
13 | mia    | 22 | 3.88
14 | nick   | 20 | 3.00
15 | olivia | 24 | 3.92
16 | peter  | 18 | 2.50
17 | queen  | 21 | 3.55
18 | ryan   | 23 | 3.15
19 | sophia | 22 | 2.70
20 | tom    | 20 | 3.35
21 | uma    | 19 | 3.05
22 | victor | 24 | 3.80
23 | wendy  | 18 | 3.45
24 | xavier | 25 | 2.90
25 | yuki   | 21 | 3.70
26 | zoe    | 20 | 3.99
27 | anna   | 23 | 2.60
28 | brian  | 22 | 3.33
29 | clara  | 19 | 3.58
30 | daniel | 18 | 3.22
31 | eric   | 26 | 3.11
32 | fiona  | 25 | 3.77
33 | george | 24 | 2.88
34 | helen  | 23 | 3.66
35 | iris   | 22 | 3.44
36 | john   | 21 | 3.01
```


## 2. COUNT 全表检查

### 输入

```sql
select count(*) from t;
```

### 预期输出

```text
count
36
```

---

# 五、Projection 指定列检查

## 1. 查询指定列

### 输入

```sql
select id, name from t where age = 20;
```

### 预期输出

```text
id | name
4  | david
8  | henry
14 | nick
20 | tom
26 | zoe
```

---

## 2. 带表名前缀的 Projection

### 输入

```sql
select t.id, t.name from t where t.age > 23;
```

### 预期输出

```text
t.id | t.name
15   | olivia
22   | victor
24   | xavier
31   | eric
32   | fiona
33   | george
```

---

# 六、WHERE 条件检查

## 1. 等值查询：整数

### 输入

```sql
select * from t where age = 19;
```

### 预期输出

```text
id | name  | age | gpa
2  | bob   | 19  | 3.65
7  | grace | 19  | 3.90
12 | leo   | 19  | 3.40
21 | uma   | 19  | 3.05
29 | clara | 19  | 3.58
```

---

## 2. 等值查询：字符串

### 输入

```sql
select id, name, age from t where name = 'alice';
```

### 预期输出

```text
id | name  | age
1  | alice | 18
```

---

## 3. 大于查询

### 输入

```sql
select id, name, age from t where age > 23;
```

### 预期输出

```text
id | name   | age
15 | olivia | 24
22 | victor | 24
24 | xavier | 25
31 | eric   | 26
32 | fiona  | 25
33 | george | 24
```

---

## 4. 大于等于查询

### 输入

```sql
select id, name, gpa from t where gpa >= 3.80;
```

### 预期输出

```text
id | name   | gpa
3  | cathy  | 3.86
7  | grace  | 3.90
13 | mia    | 3.88
15 | olivia | 3.92
22 | victor | 3.80
26 | zoe    | 3.99
```

---

## 5. 小于查询

### 输入

```sql
select id, name, gpa from t where gpa < 2.90;
```

### 预期输出

```text
id | name   | gpa
4  | david  | 2.34
8  | henry  | 2.80
16 | peter  | 2.50
19 | sophia | 2.70
27 | anna   | 2.60
33 | george | 2.88
```

---

## 6. 小于等于查询

### 输入

```sql
select id, name, age from t where age <= 18;
```

### 预期输出

```text
id | name   | age
1  | alice  | 18
3  | cathy  | 18
9  | ivy    | 18
16 | peter  | 18
23 | wendy  | 18
30 | daniel | 18
```

---

# 七、AND / OR 逻辑检查

## 1. AND 条件

### 输入

```sql
select id, name, age, gpa from t where age > 20 and gpa >= 3.50;
```

### 预期输出

```text
id | name   | age | gpa
10 | jack   | 23  | 3.75
13 | mia    | 22  | 3.88
15 | olivia | 24  | 3.92
17 | queen  | 21  | 3.55
22 | victor | 24  | 3.80
25 | yuki   | 21  | 3.70
32 | fiona  | 25  | 3.77
34 | helen  | 23  | 3.66
```

---

## 2. OR 条件

### 输入

```sql
select id, name, age from t where age = 18 or name = 'tom';
```

### 预期输出

```text
id | name   | age
1  | alice  | 18
3  | cathy  | 18
9  | ivy    | 18
16 | peter  | 18
20 | tom    | 20
23 | wendy  | 18
30 | daniel | 18
```

---

## 3. AND + OR 混合条件

如果你们支持括号，必须测试这个。

### 输入

```sql
select id, name, age, gpa from t where age = 18 or age = 19 and gpa > 3.60;
```

### 推荐预期逻辑

如果按 SQL 标准优先级，`AND` 高于 `OR`，等价于：

```sql
age = 18 or (age = 19 and gpa > 3.60)
```

### 预期输出

```text
id | name   | age | gpa
1  | alice  | 18  | 3.60
2  | bob    | 19  | 3.65
3  | cathy  | 18  | 3.86
7  | grace  | 19  | 3.90
9  | ivy    | 18  | 3.10
16 | peter  | 18  | 2.50
23 | wendy  | 18  | 3.45
30 | daniel | 18  | 3.22
```

如果你们没有做复杂优先级，展示时最好只展示明确括号版本：

```sql
select id, name, age, gpa from t where age = 18 or (age = 19 and gpa > 3.60);
```

---

# 八、COUNT 条件检查

## 1. COUNT + 等值条件

### 输入

```sql
select count(*) from t where age = 18;
```

### 预期输出

```text
count
6
```

---

## 2. COUNT + AND 条件

### 输入

```sql
select count(*) from t where age > 20 and gpa >= 3.50;
```

### 预期输出

```text
count
8
```

---

## 3. COUNT + OR 条件

### 输入

```sql
select count(*) from t where age = 18 or name = 'tom';
```

### 预期输出

```text
count
7
```
---

# 九、EXPLAIN 查询计划检查

## 1. Projection + Filter + SeqScan

### 输入

```sql
explain select t.id, t.name from t where t.age > 18;
```

### 预期输出

```text
ProjectOperator(selectItems=[t.id, t.name])
└── LogicalFilterOperator(condition=t.age > 18)
    └── TableScanOperator(table=t)
```

---

## 2. 无 WHERE 的 EXPLAIN

### 输入

```sql
explain select id, name from t;
```

### 预期输出

```text
ProjectOperator(selectItems=[id, name])
└── TableScanOperator(table=t)
```

---

# 十、UPDATE 检查

## 1. 单行 UPDATE

### 输入

```sql
update t set name = 'apple' where id = 1;

select id, name, age, gpa from t where id = 1;
```

### 预期输出

```text
update:
OK: 1 row updated

select:
id | name  | age | gpa
1  | apple | 18  | 3.60
```

---

## 2. 多条件 UPDATE

### 输入

```sql
update t set name = 'low_gpa_young' where age = 18 and gpa < 3.00;

select id, name, age, gpa from t where age = 18 and gpa < 3.00;
```

### 预期输出

```text
update:
OK: 1 row updated

select:
id | name          | age | gpa
16 | low_gpa_young | 18  | 2.50
```

---

# 十一、错误输入与异常处理检查

这些都应该报错，但程序不能退出。

## 1. 插入字段数量不匹配

### 输入

```sql
insert into t (id, name, age, gpa) values (100, 'bad', 20);
```

### 预期输出

```text
ERROR: column count does not match value count
程序继续运行。
```

---

## 2. 插入类型不匹配

### 输入

```sql
insert into t (id, name, age, gpa) values ('abc', 'bad', 20, 3.0);
```

### 预期输出

```text
ERROR: type mismatch for column id
程序继续运行。
```

---

## 3. 查询不存在字段

### 输入

```sql
select not_exist_column from t;
```

### 预期输出

```text
ERROR: column not_exist_column does not exist
程序继续运行。
```

---

## 4. WHERE 中使用不存在字段

### 输入

```sql
select * from t where not_exist_column = 1;
```

### 预期输出

```text
ERROR: column not_exist_column does not exist
程序继续运行。
```

---

## 5. UPDATE 不存在字段

### 输入

```sql
update t set not_exist_column = 1 where id = 1;
```

### 预期输出

```text
ERROR: column not_exist_column does not exist
程序继续运行。
```

---

## 6. SQL 语法错误

### 输入

```sql
select from where;
```

### 预期输出

```text
ERROR: invalid SQL syntax
程序继续运行。
```

---

# 十二、DELETE 检查

建议把 DELETE 放在所有查询测试后面，因为 DELETE 会改变数据。

## 1. DELETE + 小于条件

### 输入

```sql
delete from t where gpa < 2.90;

select count(*) from t;
```

### 被删除的记录

```text
4  | david  | 20 | 2.34
8  | henry  | 20 | 2.80
16 | low_gpa_young | 18 | 2.50
19 | sophia | 22 | 2.70
27 | anna   | 23 | 2.60
33 | george | 24 | 2.88
```

### 预期输出

```text
delete:
OK: 6 rows deleted

count:
count
30
```

---

## 2. DELETE + OR 条件

### 输入

```sql
delete from t where age = 25 or name = 'john';

select count(*) from t;
```

### 被删除的记录

```text
24 | xavier | 25 | 2.90
32 | fiona  | 25 | 3.77
36 | john   | 21 | 3.01
```

### 预期输出

```text
delete:
OK: 3 rows deleted

count:
count
27
```

---

## 3. DELETE 后确认数据不存在

### 输入

```sql
select * from t where gpa < 2.90;

select * from t where age = 25 or name = 'john';
```

### 预期输出

```text
第一条 select:
Empty set 或 0 rows

第二条 select:
Empty set 或 0 rows
```

---

# 十三、持久化检查

PDF 明确要求表和数据要持久化，修改操作要实时写入磁盘。

## 操作步骤

先执行：

```sql
show tables;

select count(*) from t;

select id, name, age, gpa from t where id = 1;
```

### 预期输出

```text
show tables:
应该包含 t

select count(*):
count
27

select id = 1:
id | name  | age | gpa
1  | apple | 18  | 3.60
```

然后：

```text
关闭程序。
重新启动程序。
再次运行下面 SQL。
```

### 输入

```sql
show tables;

describe t;

select count(*) from t;

select id, name, age, gpa from t where id = 1;

select * from t where gpa < 2.90;
```

### 预期输出

```text
show tables:
包含 t

describe t:
id int
name varchar
age int
gpa double

select count(*):
count
27

select id = 1:
id | name  | age | gpa
1  | apple | 18  | 3.60

select * where gpa < 2.90:
Empty set 或 0 rows
```

---

# 十四、DROP TABLE 检查

建议最后再做。

## 输入

```sql
drop table t;

show tables;

describe t;

select * from t;
```

### 预期输出

```text
drop table:
OK: table t dropped

show tables:
不再包含 t

describe t:
ERROR: table t does not exist

select * from t:
ERROR: table t does not exist
```

---

# 十五、进阶功能检查 SQL

下面是进阶部分。如果你们没有实现，可以跳过。

建议在执行 DELETE 和 DROP TABLE 之前跑这些进阶测试。也就是说，最好在原始 36 条数据还完整时跑。

---

# A. Aggregation：MAX / MIN / GROUP BY

## 1. MAX

### 输入

```sql
select max(gpa) from t;
```

### 预期输出

```text
max(gpa)
3.99
```

---

## 2. MIN

### 输入

```sql
select min(gpa) from t;
```

### 预期输出

```text
min(gpa)
2.34
```

---

## 3. GROUP BY

### 输入

```sql
select age, count(*) from t group by age;
```

### 预期输出

```text
age | count
18  | 6
19  | 5
20  | 5
21  | 5
22  | 5
23  | 4
24  | 3
25  | 2
26  | 1
```

顺序可以不同，但每个 age 对应的 count 必须正确。

---

# B. ORDER BY

## 输入

```sql
select id, name, age from t order by age;
```

### 预期输出

结果应该按 `age` 从小到大排列。

前几行应该是 age = 18：

```text
id | name   | age
1  | apple  | 18
3  | cathy  | 18
9  | ivy    | 18
16 | low_gpa_young | 18
23 | wendy  | 18
30 | daniel | 18
```

最后一行应该是：

```text
31 | eric | 26
```

如果你们在 update 前跑这个测试，那么 id = 1 应该是 `alice`，id = 16 应该是 `peter`。
。

---

# C. Join 检查

## 1. 创建第二张表

### 输入

```sql
create table course(  student_id int,  course_name varchar,  score double);
```

### 预期输出

```text
OK: table course created
```

---

## 2. 插入课程数据

### 输入

```sql
insert into course (student_id, course_name, score) values (1, 'db', 95.0);
insert into course (student_id, course_name, score) values (2, 'db', 88.0);
insert into course (student_id, course_name, score) values (3, 'ai', 91.0);
insert into course (student_id, course_name, score) values (7, 'db', 99.0);
insert into course (student_id, course_name, score) values (10, 'os', 87.0);
insert into course (student_id, course_name, score) values (13, 'ai', 93.0);
insert into course (student_id, course_name, score) values (15, 'db', 98.0);
insert into course (student_id, course_name, score) values (22, 'os', 90.0);
insert into course (student_id, course_name, score) values (26, 'ai', 96.0);
insert into course (student_id, course_name, score) values (40, 'db', 70.0);
```

### 预期输出

每条：

```text
OK: 1 row inserted
```

---

## 3. Nested Loop Join

如果你们支持标准 join：

### 输入

```sql
select t.id, t.name, course.course_name, course.score from t join course on t.id = course.student_id;
```

### 预期输出

```text
t.id | t.name  | course.course_name | course.score
1    | apple   | db                 | 95.0
2    | bob     | db                 | 88.0
3    | cathy   | ai                 | 91.0
7    | grace   | db                 | 99.0
10   | jack    | os                 | 87.0
13   | mia     | ai                 | 93.0
15   | olivia  | db                 | 98.0
22   | victor  | os                 | 90.0
26   | zoe     | ai                 | 96.0
```



---

# D. IN / NOT IN / EXISTS

## 1. IN

### 输入

```sql
select id, name, age from t where age in (18, 19);
```

### 预期输出

应返回 11 行：

```text
age = 18 的 6 行
age = 19 的 5 行
```

具体：

```text
1  | apple  | 18
2  | bob    | 19
3  | cathy  | 18
7  | grace  | 19
9  | ivy    | 18
12 | leo    | 19
16 | low_gpa_young | 18
21 | uma    | 19
23 | wendy  | 18
29 | clara  | 19
30 | daniel | 18
```

如果你们在 update 前运行，则 `apple` 应为 `alice`，`low_gpa_young` 应为 `peter`。

---

## 2. NOT IN

### 输入

```sql
select id, name, age from t where age not in (18, 19, 20, 21, 22);
```

### 预期输出

应返回 age 为 23、24、25、26 的记录，共 10 行：

```text
10 | jack   | 23
15 | olivia | 24
18 | ryan   | 23
22 | victor | 24
24 | xavier | 25
27 | anna   | 23
31 | eric   | 26
32 | fiona  | 25
33 | george | 24
34 | helen  | 23
```

---

## 3. EXISTS

如果支持相关子查询：

### 输入

```sql
select id, name from t where exists (  select * from course where course.student_id = t.id);
```

### 预期输出

```text
id | name
1  | apple
2  | bob
3  | cathy
7  | grace
10 | jack
13 | mia
15 | olivia
22 | victor
26 | zoe
```

如果在 update 前运行，`apple` 应为 `alice`。

---

# E. ALTER TABLE

如果你们实现了部分 ALTER TABLE，可以测试：

### 输入

```sql
alter table t add column email varchar;

describe t;
```

### 预期输出

```text
OK: column email added

describe t:
Field | Type
id    | int
name  | varchar
age   | int
gpa   | double
email | varchar
```

然后测试旧数据：

```sql
select id, name, email from t where id = 1;
```

### 预期输出

```text
id | name  | email
1  | apple | null
```

或者：

```text
1 | apple | 
```

只要你们设计中明确旧数据新增字段的默认值即可。


---

# F. Index / B+ Tree 检查

## 1. 创建索引

### 输入

```sql
create index idx_age on t(age);
```

### 预期输出

```text
OK: index idx_age created on t(age)
```

内部应完成：

```text
metadata / JSON 中记录 index
内存中建立 B+ Tree
把已有 36 条数据插入 B+ Tree
```

---

## 2. 重复创建同名索引

### 输入

```sql
create index idx_age on t(age);
```

### 预期输出

```text
ERROR: index idx_age already exists
程序继续运行。
```

---

## 3. 对不存在字段创建索引

### 输入

```sql
create index idx_bad on t(not_exist_column);
```

### 预期输出

```text
ERROR: column not_exist_column does not exist
程序继续运行。
```

---

## 4. 使用索引查询

### 输入

```sql
explain select id, name from t where age = 19;
```

### 理想预期输出

如果 optimizer 接入了 index：

```text
ProjectOperator(selectItems=[id, name])
└── IndexScanOperator(table=t, index=idx_age, condition=age = 19)
```

如果你们只实现了 B+ Tree，但没有接入 optimizer，也可以输出：

```text
ProjectOperator
└── FilterOperator(condition=age = 19)
    └── TableScanOperator(table=t)
```

但展示时要说明：索引已实现，但查询计划尚未替换为 IndexScan。

---

## 5. 索引正确性检查

### 输入

```sql
select id, name from t where age = 19;
```

### 预期输出

```text
id | name
2  | bob
7  | grace
12 | leo
21 | uma
29 | clara
```

---

## 6. 插入后索引动态更新

### 输入

```sql
insert into t (id, name, age, gpa) values (37, 'new_age_19', 19, 3.33);

select id, name from t where age = 19;
```

### 预期输出

```text
id | name
2  | bob
7  | grace
12 | leo
21 | uma
29 | clara
37 | new_age_19
```

易错点：

* create index 只对已有数据有效。
* 后续 insert 没有更新 B+ Tree。
* select 走索引时查不到新插入数据。

---

## 7. 删除后索引动态更新

### 输入

```sql
delete from t where id = 37;

select id, name from t where age = 19;
```

### 预期输出

```text
id | name
2  | bob
7  | grace
12 | leo
21 | uma
29 | clara
```

易错点：

* 数据删了，但索引里还留着 id = 37。
* IndexScan 查出已经删除的数据。
* B+ Tree 删除后节点结构损坏。

---

## 8. 删除索引

### 输入

```sql
drop index idx_age;
```

### 预期输出

```text
OK: index idx_age dropped
```

---

## 9. 删除不存在索引

### 输入

```sql
drop index idx_age;
```

### 预期输出

```text
ERROR: index idx_age does not exist
程序继续运行。
```

---

# G. Transaction 检查

如果你们支持 SQL 命令形式的 transaction，可以这样测。

## 1. Rollback

### 输入

```sql
begin;

update t set name = 'rollback_test' where id = 2;

select id, name from t where id = 2;

rollback;

select id, name from t where id = 2;
```

### 预期输出

```text
begin:
OK: transaction started

update:
OK: 1 row updated

select before rollback:
id | name
2  | rollback_test

rollback:
OK: transaction rolled back

select after rollback:
id | name
2  | bob
```

易错点：

* rollback 只回滚内存，不回滚磁盘。
* rollback 后 select 仍然是 rollback_test。
* rollback 后数据文件损坏。

---

## 2. Commit

### 输入

```sql
begin;

update t set name = 'commit_test' where id = 2;

commit;

select id, name from t where id = 2;
```

### 预期输出

```text
commit:
OK: transaction committed

select:
id | name
2  | commit_test
```

然后重启程序，再运行：

```sql
select id, name from t where id = 2;
```

### 预期输出

```text
id | name
2  | commit_test
```

易错点：

* commit 后没有持久化。
* 重启后回到 bob。
* commit 清理 undo log 不完整。

---

## 3. Savepoint

### 输入

```sql
begin;

update t set name = 'sp_before' where id = 3;

savepoint s1;

update t set name = 'sp_after' where id = 3;

select id, name from t where id = 3;

rollback to savepoint s1;

select id, name from t where id = 3;

release savepoint s1;

commit;
```

### 预期输出

```text
select before rollback to savepoint:
id | name
3  | sp_after

select after rollback to savepoint:
id | name
3  | sp_before

commit:
OK
```

易错点：

* rollback to savepoint 直接回滚到 begin。
* release savepoint 后仍然可以 rollback 到该 savepoint。
* savepoint 名称管理错误。
* 多个 savepoint 顺序错乱。

---

# 十六、最终展示推荐顺序

你们真正 demo 时，不建议把所有异常测试都现场跑一遍。可以按下面顺序展示，最稳：

```sql
show tables;

create table t(
  id int,
  name varchar,
  age int,
  gpa double
);

describe t;

insert into t (id, name, age, gpa) values (1, 'alice', 18, 3.60);
-- 后面批量插入 36 条

select count(*) from t;

select * from t;

select id, name from t where age = 20;

select t.id, t.name from t where t.age > 23;

select * from t where age = 19;

select id, name, gpa from t where gpa >= 3.80;

select id, name, age, gpa from t where age > 20 and gpa >= 3.50;

select id, name, age from t where age = 18 or name = 'tom';

select count(*) from t where age > 20 and gpa >= 3.50;

explain select t.id, t.name from t where t.age > 18;

update t set name = 'apple' where id = 1;

select id, name, age, gpa from t where id = 1;

delete from t where gpa < 2.90;

select count(*) from t;

show tables;
```

然后重启程序，展示：

```sql
show tables;

describe t;

select count(*) from t;

select id, name, age, gpa from t where id = 1;
```

这个重启后的检查非常关键，因为它能证明：

```text
CREATE TABLE 持久化成功
INSERT 持久化成功
UPDATE 持久化成功
DELETE 持久化成功
catalog 持久化成功
```

---

# 十七、展示时最容易被老师问的问题

你们可以提前准备这些回答：

1. `select id, name from t where age > 18;` 的执行计划是什么？
2. 为什么 Filter 应该在 Project 前面？
3. SeqScan 是怎么扫描数据的？
4. SeqScan 如何判断一条记录是否满足 WHERE？
5. `AND` 和 `OR` 是怎么表示的？表达式树怎么设计？
6. `COUNT(*)` 是单独实现，还是复用 SeqScan + Filter？
7. DELETE 删除记录后，磁盘文件如何更新？
8. UPDATE 是原地修改，还是重写整张表？
9. 重启后数据为什么还在？
10. LRU 的 `Pin` / `Unpin` / `Victim` 分别做什么？
11. Clock Replacer 为什么能近似 LRU？
12. 如果做了 Index：B+ Tree 如何支持等值查询和范围查询？
13. 如果做了 Transaction：rollback 和 savepoint 如何恢复旧数据？

这套 SQL 基本可以作为你们的最终自测脚本和展示脚本。基础部分全部跑通，项目的基础分会比较稳。
