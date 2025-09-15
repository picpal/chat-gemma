-- 사용자 상태를 APPROVED로 변경하는 SQL
UPDATE users SET status = 'APPROVED', approved_at = CURRENT_TIMESTAMP WHERE email = 'test2@example.com';