/*
 * Copyright (c) 2021 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- CREATE TYPE upload_status as enum ('SUBMITTED', 'PROCESSING', 'COMPLETE', 'ERROR'); todo: only do this if not exists (flyway?)

CREATE TABLE if not exists submission
(
    submission_id       uuid        DEFAULT uuid_generate_v4(),
    user_id             uuid   not null,
    created_at          timestamptz DEFAULT current_timestamp,
    total_records       int    not null,
    original_file_names text[] not null,
    PRIMARY KEY (submission_id)
);

CREATE TABLE if not exists upload
(
    upload_id           uuid        DEFAULT uuid_generate_v4(),
    study_id            VARCHAR(32)   not null,
    submitter_sample_id VARCHAR(32)   not null,
    submission_id       uuid          not null,
    user_id             uuid          not null,
    created_at          timestamptz DEFAULT current_timestamp,
    status              upload_status not null,
    analysis_Id         uuid,
    error               text,
    original_file_pair  text[]        not null,
    PRIMARY KEY (upload_id),
    CONSTRAINT fk_submission
        FOREIGN KEY (submission_id)
            REFERENCES submission (submission_id)
);
