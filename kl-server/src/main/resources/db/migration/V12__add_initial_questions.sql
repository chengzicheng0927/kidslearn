-- V12: 初始化前端的 mock 题目数据

INSERT INTO questions (
    subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode, audit_status
) VALUES (
    'en', '动物词汇', 'EN_VOCAB_ANIMALS', 'medium', '英语 · 动物词汇 · ⭐⭐', 'Which animal lives in the ocean and has eight arms?',
    '[{"id":"q1-a","text":"Shark","emoji":"🦈"},{"id":"q1-b","text":"Octopus","emoji":"🐙"},{"id":"q1-c","text":"Dolphin","emoji":"🐬"},{"id":"q1-d","text":"Crab","emoji":"🦀"}]'::jsonb, 'q1-b', 'Octopus means 章鱼. It lives in the ocean and has eight arms.',
    '{"先看关键词 ocean，它说明动物生活在海里。","再看 eight arms，表示它有八条手臂。","四个选项里只有 Octopus 同时满足这两个条件，所以答案是 Octopus。"}', '你已经抓到关键词啦，下次会更快答出来。', 4, '{"stars":8,"xp":18,"xpNeeded":100,"level":4}'::jsonb,
    'correct', 'approved'
);

INSERT INTO questions (
    subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode, audit_status
) VALUES (
    'en', '水果词汇', 'EN_VOCAB_FRUITS', 'medium', '英语 · 水果词汇 · ⭐⭐', 'Which fruit is red and wears a leafy crown?',
    '[{"id":"q2-a","text":"Apple","emoji":"🍎"},{"id":"q2-b","text":"Banana","emoji":"🍌"},{"id":"q2-c","text":"Orange","emoji":"🍊"},{"id":"q2-d","text":"Strawberry","emoji":"🍓"}]'::jsonb, 'q2-d', 'Strawberries are red and their green leafy top looks like a tiny crown.',
    '{"题目先告诉我们 fruit，而且颜色是 red，所以先排除香蕉。","wears a leafy crown 的意思是头上有绿色叶片，像戴着小皇冠。","Strawberry 顶部正好有一圈绿色叶片，所以正确答案是 Strawberry。"}', '你已经注意到颜色线索了，再把外形线索一起看就更棒了。', 5, '{"stars":10,"xp":20,"xpNeeded":100,"level":4,"badge":{"icon":"🏅","title":"词汇小达人 徽章解锁！","sub":"累计完成 2 道英语词汇题"}}'::jsonb,
    'reward', 'approved'
);

INSERT INTO questions (
    subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode, audit_status
) VALUES (
    'en', '颜色词汇', 'EN_VOCAB_COLORS', 'easy', '英语 · 颜色词汇 · ⭐', 'What color is the sky on a sunny day?',
    '[{"id":"q3-a","text":"Red","emoji":"🔴"},{"id":"q3-b","text":"Blue","emoji":"🔵"},{"id":"q3-c","text":"Green","emoji":"🟢"},{"id":"q3-d","text":"Yellow","emoji":"🟡"}]'::jsonb, 'q3-b', 'On a clear sunny day, the sky looks blue.',
    '{"题目问的是 sunny day，也就是晴天。","晴天时我们看到的天空通常是蓝色的。","所以正确答案是 Blue。"}', '这种生活里的题目很适合你，多观察就会越来越准。', 3, '{"stars":6,"xp":14,"xpNeeded":100,"level":4}'::jsonb,
    'correct', 'approved'
);

INSERT INTO questions (
    subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode, audit_status
) VALUES (
    'ma', '10 以内加法', 'MA_ADD_10', 'easy', '数学 · 10 以内加法 · ⭐', '3 + 4 = ?',
    '[{"id":"q4-a","text":"5"},{"id":"q4-b","text":"6"},{"id":"q4-c","text":"7"},{"id":"q4-d","text":"8"}]'::jsonb, 'q4-c', '3 plus 4 equals 7.',
    '{"从 3 开始往后数 4 个数：4、5、6、7。","或者把 3 和 4 两部分合在一起，一共是 7。","所以 3 + 4 = 7。"}', '你已经很接近了，再多练几道加法题会更熟练。', 3, '{"stars":7,"xp":15,"xpNeeded":100,"level":4}'::jsonb,
    'correct', 'approved'
);

INSERT INTO questions (
    subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode, audit_status
) VALUES (
    'en', '食物词汇', 'EN_VOCAB_FOOD', 'easy', '英语 · 食物词汇 · ⭐', 'What do you call a baby dog?',
    '[{"id":"q5-a","text":"Kitten","emoji":"🐱"},{"id":"q5-b","text":"Calf","emoji":"🐮"},{"id":"q5-c","text":"Puppy","emoji":"🐶"},{"id":"q5-d","text":"Cub","emoji":"🐻"}]'::jsonb, 'q5-c', 'A baby dog is called a Puppy.',
    '{"题目问的是 baby dog，小狗的宝宝。","Kitten 是小猫，Calf 是小牛，Cub 一般指熊宝宝。","Puppy 正好是小狗的意思，所以答案是 Puppy。"}', '动物宝宝的词汇很实用，多积累一些吧！', 4, '{"stars":7,"xp":14,"xpNeeded":100,"level":4}'::jsonb,
    'correct', 'approved'
);

INSERT INTO questions (
    subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode, audit_status
) VALUES (
    'en', '天气词汇', 'EN_VOCAB_WEATHER', 'easy', '英语 · 天气词汇 · ⭐', 'Which word describes the weather when the sun is shining and it is hot?',
    '[{"id":"q6-a","text":"Rainy","emoji":"🌧️"},{"id":"q6-b","text":"Windy","emoji":"🌬️"},{"id":"q6-c","text":"Sunny","emoji":"☀️"},{"id":"q6-d","text":"Snowy","emoji":"❄️"}]'::jsonb, 'q6-c', 'Sunny describes hot weather with the sun shining.',
    '{"题目描述了两个线索：sun is shining 和 it is hot。","Rainy 是下雨，Windy 是刮风，Snowy 是下雪。","Sunny 正好描述晴天、太阳很晒的天气，所以答案是 Sunny。"}', '天气词汇很实用，出门时记得用一用！', 4, '{"stars":7,"xp":14,"xpNeeded":100,"level":4}'::jsonb,
    'correct', 'approved'
);

INSERT INTO questions (
    subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode, audit_status
) VALUES (
    'en', '数字词汇', 'EN_VOCAB_NUMBERS', 'medium', '英语 · 数字词汇 · ⭐⭐', 'How many sides does a triangle have?',
    '[{"id":"q7-a","text":"Two","emoji":"2️⃣"},{"id":"q7-b","text":"Three","emoji":"3️⃣"},{"id":"q7-c","text":"Four","emoji":"4️⃣"},{"id":"q7-d","text":"Five","emoji":"5️⃣"}]'::jsonb, 'q7-b', 'Triangle means three angles, so it has three sides.',
    '{"Triangle 前缀 tri- 表示\"三\"。","它代表有三个角的形状，自然就有三条边。","所以正确答案是 Three。"}', '你把形状和英语前缀结合起来了，真棒！', 5, '{"stars":10,"xp":20,"xpNeeded":100,"level":4}'::jsonb,
    'reward', 'approved'
);

INSERT INTO questions (
    subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode, audit_status
) VALUES (
    'en', '身体部位', 'EN_VOCAB_BODY', 'easy', '英语 · 身体部位 · ⭐', 'Which body part do you use to hear sounds?',
    '[{"id":"q8-a","text":"Eyes","emoji":"👀"},{"id":"q8-b","text":"Ears","emoji":"👂"},{"id":"q8-c","text":"Nose","emoji":"👃"},{"id":"q8-d","text":"Mouth","emoji":"👄"}]'::jsonb, 'q8-b', 'Ears are the body parts we use to hear sounds.',
    '{"题目问的是我们用什么来听声音。","Eyes 是眼睛，用来看；Ears 是耳朵，用来听。","所以正确答案是 Ears。"}', '身体部位的词汇在日常交流中很有用！', 3, '{"stars":6,"xp":12,"xpNeeded":100,"level":4}'::jsonb,
    'correct', 'approved'
);

INSERT INTO questions (
    subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode, audit_status
) VALUES (
    'en', '衣服词汇', 'EN_VOCAB_CLOTHES', 'easy', '英语 · 衣服词汇 · ⭐', 'What do you wear on your feet when you go to school?',
    '[{"id":"q15-a","text":"Gloves","emoji":"🧤"},{"id":"q15-b","text":"Socks","emoji":"🧦"},{"id":"q15-c","text":"Shoes","emoji":"👟"},{"id":"q15-d","text":"Hat","emoji":"🧢"}]'::jsonb, 'q15-c', 'Shoes are what we wear on our feet to go to school.',
    '{"题目问的是去学校时穿在脚上的东西。","Gloves 是手套，Socks 是袜子，Shoes 是鞋子，Hat 是帽子。","去学校需要穿鞋，正确答案是 Shoes。"}', '衣物词汇也很有用，出门时可以认一认！', 3, '{"stars":6,"xp":12,"xpNeeded":100,"level":4}'::jsonb,
    'correct', 'approved'
);

INSERT INTO questions (
    subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode, audit_status
) VALUES (
    'ma', '简单应用题', 'MA_WORD_PROBLEM', 'easy', '数学 · 简单应用题 · ⭐', '小明有 3 个苹果，小红又给了他 2 个，现在小明有多少个苹果？',
    '[{"id":"q16-a","text":"4"},{"id":"q16-b","text":"5"},{"id":"q16-c","text":"6"},{"id":"q16-d","text":"7"}]'::jsonb, 'q16-b', '3 + 2 = 5，小明现在有 5 个苹果。',
    '{"小明原来有 3 个苹果。","小红又给了他 2 个，所以用加法：3 + 2。","3 + 2 = 5，答案是 5 个苹果。"}', '应用题就是把生活变成数学题，你做得很棒！', 4, '{"stars":8,"xp":16,"xpNeeded":100,"level":4}'::jsonb,
    'correct', 'approved'
);

INSERT INTO questions (
    subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode, audit_status
) VALUES (
    'ma', '10 以内减法', 'MA_SUB_10', 'easy', '数学 · 10 以内减法 · ⭐', '9 - 3 = ?',
    '[{"id":"q9-a","text":"4"},{"id":"q9-b","text":"5"},{"id":"q9-c","text":"6"},{"id":"q9-d","text":"7"}]'::jsonb, 'q9-c', '9 minus 3 equals 6.',
    '{"从 9 往前数 3 个数：8、7、6。","所以 9 - 3 = 6。"}', '减法学得真快！减法就是加法的逆运算。', 3, '{"stars":6,"xp":14,"xpNeeded":100,"level":4}'::jsonb,
    'correct', 'approved'
);

INSERT INTO questions (
    subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode, audit_status
) VALUES (
    'ma', '形状认知', 'MA_SHAPE', 'easy', '数学 · 形状认知 · ⭐', 'How many corners does a square have?',
    '[{"id":"q10-a","text":"3"},{"id":"q10-b","text":"4"},{"id":"q10-c","text":"5"},{"id":"q10-d","text":"6"}]'::jsonb, 'q10-b', 'A square has four equal sides and four corners.',
    '{"正方形有四条边，每两条边相交的地方就是一个角。","正方形四条边，所以有四个角。","答案是 4。"}', '形状和数数的结合真棒！', 3, '{"stars":8,"xp":16,"xpNeeded":100,"level":4}'::jsonb,
    'reward', 'approved'
);

INSERT INTO questions (
    subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode, audit_status
) VALUES (
    'ma', '10 以内加法', 'MA_ADD_10', 'easy', '数学 · 10 以内加法 · ⭐', '5 + 5 = ?',
    '[{"id":"q11-a","text":"8"},{"id":"q11-b","text":"9"},{"id":"q11-c","text":"10"},{"id":"q11-d","text":"11"}]'::jsonb, 'q11-c', '5 plus 5 equals 10.',
    '{"两个 5 加在一起。","5 + 5 = 10。"}', '配对加法很方便，比如两只手各有5根手指！', 2, '{"stars":5,"xp":12,"xpNeeded":100,"level":4}'::jsonb,
    'correct', 'approved'
);

INSERT INTO questions (
    subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode, audit_status
) VALUES (
    'ma', '10 以内减法', 'MA_SUB_10', 'easy', '数学 · 10 以内减法 · ⭐', '10 - 6 = ?',
    '[{"id":"q12-a","text":"2"},{"id":"q12-b","text":"3"},{"id":"q12-c","text":"4"},{"id":"q12-d","text":"5"}]'::jsonb, 'q12-c', '10 minus 6 equals 4.',
    '{"10 减 6，从 10 往前数 6 个数：9、8、7、6、5、4。","所以答案是 4。"}', '大数减法也没难倒你！', 4, '{"stars":7,"xp":15,"xpNeeded":100,"level":4}'::jsonb,
    'correct', 'approved'
);

INSERT INTO questions (
    subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode, audit_status
) VALUES (
    'ma', '找规律', 'MA_PATTERN', 'medium', '数学 · 找规律 · ⭐⭐', '2, 4, 6, 8, ? — What comes next?',
    '[{"id":"q13-a","text":"9"},{"id":"q13-b","text":"10"},{"id":"q13-c","text":"11"},{"id":"q13-d","text":"12"}]'::jsonb, 'q13-b', 'The pattern is adding 2 each time: 2, 4, 6, 8, 10.',
    '{"观察规律：2+2=4，4+2=6，6+2=8。","每次加 2，所以下一个是 8+2=10。","答案是 10。"}', '找规律需要仔细观察，你做到了！', 5, '{"stars":10,"xp":20,"xpNeeded":100,"level":4}'::jsonb,
    'correct', 'approved'
);

INSERT INTO questions (
    subject, topic, topic_key, difficulty, label, question, options, correct_answer_id,
    explanation, explanation_steps, encouragement, estimated_minutes, reward, success_result_mode, audit_status
) VALUES (
    'ma', '10 以内加法', 'MA_ADD_10', 'easy', '数学 · 10 以内加法 · ⭐', '7 + 2 = ?',
    '[{"id":"q14-a","text":"8"},{"id":"q14-b","text":"9"},{"id":"q14-c","text":"10"},{"id":"q14-d","text":"11"}]'::jsonb, 'q14-b', '7 plus 2 equals 9.',
    '{"从 7 往后数 2 个数：8、9。","所以 7 + 2 = 9。"}', '又答对了，继续加油！', 2, '{"stars":5,"xp":12,"xpNeeded":100,"level":4}'::jsonb,
    'correct', 'approved'
);
