CREATE TABLE profiles (
                          id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                          name        VARCHAR(50)  NOT NULL,
                          avatar_key  VARCHAR(50)  NOT NULL DEFAULT 'avatar_1',
                          kids        BOOLEAN      NOT NULL DEFAULT FALSE,
                          created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                          updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- one name per account, ignoring upper/lower case ("Kids" and "kids" count as the same)
CREATE UNIQUE INDEX uq_profiles_user_name ON profiles (user_id, LOWER(name));