CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
DO $$
DECLARE
    _subject_user_id VARCHAR(36);
    _new_profile_id UUID;
    _profile_exists BOOLEAN;
    _link_exists BOOLEAN;
BEGIN
    FOR _subject_user_id IN
        SELECT DISTINCT subject_user_id::VARCHAR FROM transaction
    LOOP
        -- Check if an entry already exists in individual_profile
        SELECT EXISTS(SELECT 1 FROM individual_profile WHERE owner_user_id::VARCHAR = _subject_user_id)
        INTO _profile_exists;

        IF NOT _profile_exists THEN
            -- Insert into individual_profile and capture the new profile id
            INSERT INTO individual_profile (id, owner_user_id, created_by, last_updated_by, created_timestamp, last_updated_timestamp)
            VALUES (uuid_generate_v4(), _subject_user_id::UUID, 'access_profile_update', 'access_profile_update', now(), now())
            RETURNING id INTO _new_profile_id;

            -- Insert into individual_user_link using the new profile id
            INSERT INTO individual_user_link (id, profile_id, user_id, access, created_by, last_updated_by, created_timestamp, last_updated_timestamp)
            VALUES (uuid_generate_v4(), _new_profile_id, _subject_user_id::UUID, 'ADMIN', 'access_profile_update', 'access_profile_update', now(), now());
        ELSE
            -- Get existing profile id
            SELECT id INTO _new_profile_id FROM individual_profile WHERE owner_user_id::VARCHAR = _subject_user_id;
        END IF;

        -- Check if an entry already exists in individual_user_link
        SELECT EXISTS(SELECT 1 FROM individual_user_link WHERE user_id::VARCHAR = _subject_user_id AND profile_id = _new_profile_id)
        INTO _link_exists;

        IF NOT _link_exists THEN
            -- Insert into individual_user_link using the existing profile id
            INSERT INTO individual_user_link (id, profile_id, user_id, access, created_by, last_updated_by, created_timestamp, last_updated_timestamp)
            VALUES (uuid_generate_v4(), _new_profile_id, _subject_user_id::UUID, 'ADMIN', 'access_profile_update', 'access_profile_update', now(), now());
        END IF;

        -- Update the transaction table
        UPDATE transaction
        SET subject_profile_id = _new_profile_id, subject_profile_type = 'INDIVIDUAL'
        WHERE subject_user_id::VARCHAR = _subject_user_id AND (subject_profile_id IS NULL OR subject_profile_type IS NULL);
    END LOOP;

    -- Alter transaction table to make columns NOT NULL
    ALTER TABLE transaction ALTER COLUMN subject_profile_id SET NOT NULL;
    ALTER TABLE transaction ALTER COLUMN subject_profile_type SET NOT NULL;
END $$;
