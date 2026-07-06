-------------------------------------------------------
-- Region Master Data
-------------------------------------------------------

IF NOT EXISTS (
    SELECT 1
    FROM region
    WHERE name = N'서울'
      AND parent_region_id IS NULL
)
BEGIN
    INSERT INTO region (parent_region_id, name, level)
    VALUES (NULL, N'서울', 'METROPOLITAN');
END;

DECLARE @SEOUL_REGION_ID BIGINT;
SELECT @SEOUL_REGION_ID = region_id
FROM region
WHERE name = N'서울'
  AND parent_region_id IS NULL;

IF NOT EXISTS (
    SELECT 1
    FROM region
    WHERE name = N'양천'
      AND parent_region_id = @SEOUL_REGION_ID
)
BEGIN
    INSERT INTO region (parent_region_id, name, level)
    VALUES (@SEOUL_REGION_ID, N'양천', 'OPERATION');
END;

IF NOT EXISTS (
    SELECT 1
    FROM region
    WHERE name = N'강북'
      AND parent_region_id = @SEOUL_REGION_ID
)
BEGIN
    INSERT INTO region (parent_region_id, name, level)
    VALUES (@SEOUL_REGION_ID, N'강북', 'OPERATION');
END;

IF NOT EXISTS (
    SELECT 1
    FROM region
    WHERE name = N'충청남도'
      AND parent_region_id IS NULL
)
BEGIN
    INSERT INTO region (parent_region_id, name, level)
    VALUES (NULL, N'충청남도', 'METROPOLITAN');
END;

DECLARE @CHUNGCHEONGNAM_REGION_ID BIGINT;
SELECT @CHUNGCHEONGNAM_REGION_ID = region_id
FROM region
WHERE name = N'충청남도'
  AND parent_region_id IS NULL;

IF NOT EXISTS (
    SELECT 1
    FROM region
    WHERE name = N'천안'
      AND parent_region_id = @CHUNGCHEONGNAM_REGION_ID
)
BEGIN
    INSERT INTO region (parent_region_id, name, level)
    VALUES (@CHUNGCHEONGNAM_REGION_ID, N'천안', 'OPERATION');
END;

IF NOT EXISTS (
    SELECT 1
    FROM region
    WHERE name = N'경기도'
      AND parent_region_id IS NULL
)
BEGIN
    INSERT INTO region (parent_region_id, name, level)
    VALUES (NULL, N'경기도', 'METROPOLITAN');
END;

DECLARE @GYEONGGI_REGION_ID BIGINT;
SELECT @GYEONGGI_REGION_ID = region_id
FROM region
WHERE name = N'경기도'
  AND parent_region_id IS NULL;

IF NOT EXISTS (
    SELECT 1
    FROM region
    WHERE name = N'의정부'
      AND parent_region_id = @GYEONGGI_REGION_ID
)
BEGIN
    INSERT INTO region (parent_region_id, name, level)
    VALUES (@GYEONGGI_REGION_ID, N'의정부', 'OPERATION');
END;
