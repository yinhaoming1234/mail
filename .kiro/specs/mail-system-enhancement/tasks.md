# Implementation Plan

## Phase 1: Admin Web API Enhancement

- [x] 1. Add REST API for user registration
  - [x] 1.1 Create RegisterRequest and RegisterResponse DTOs
    - Add DTOs in `admin-web/src/main/java/com/yhm/adminweb/dto/`
    - Include username, domain, password fields with validation annotations
    - _Requirements: 1.1, 1.3, 1.4_
  - [x] 1.2 Create ApiController with register endpoint
    - Add `@RestController` at `/api/register`
    - Implement email format validation
    - Implement password length validation (≥6 chars)
    - Return appropriate error codes (400, 409)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  - [x] 1.3 Write property test for registration validation
    - **Property 2: Email validation rejects invalid formats**
    - **Property 3: Password length validation**
    - **Validates: Requirements 1.3, 1.4**
  - [x] 1.4 Write property test for user creation
    - **Property 1: Registration creates valid user**
    - **Validates: Requirements 1.1, 1.5**

- [-] 2. Add REST API for password change
  - [x] 2.1 Create PasswordChangeRequest DTO
    - Include email, currentPassword, newPassword fields
    - _Requirements: 12.1_
  - [ ] 2.2 Add password change endpoint to ApiController
    - Add `POST /api/password/change` endpoint
    - Verify current password before update
    - Return 401 for wrong current password
    - _Requirements: 12.1, 12.2, 12.3_
  - [ ] 2.3 Write property test for password reset
    - **Property 12: Password reset updates authentication**
    - **Validates: Requirements 6.5, 12.2**

- [-] 3. Add REST API for domains list
  - [ ] 3.1 Create DomainDto and add domains endpoint
    - Add `GET /api/domains` endpoint
    - Return only enabled domains
    - _Requirements: 7.1_

- [ ] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Phase 2: Android Client Registration Feature

- [-] 5. Implement Admin API client
  - [ ] 5.1 Add Retrofit dependencies and create AdminApiService
    - Add Retrofit and OkHttp dependencies to build.gradle.kts
    - Create AdminApiService interface with suspend functions
    - Configure base URL to 192.168.1.106:8080
    - _Requirements: 1.1, 13.1_
  - [ ] 5.2 Create API data classes
    - Create RegisterRequest, RegisterResponse, PasswordChangeRequest
    - Add proper JSON serialization annotations
    - _Requirements: 1.1, 12.1_

- [-] 6. Update EmailRepository with registration
  - [ ] 6.1 Add AdminApiService to EmailRepository
    - Inject AdminApiService into repository
    - Add register() function
    - Add changePassword() function
    - _Requirements: 1.1, 12.1_
  - [ ] 6.2 Write unit tests for repository registration
    - Test successful registration flow
    - Test error handling
    - _Requirements: 1.1, 1.2_

- [-] 7. Implement RegisterScreen functionality
  - [ ] 7.1 Connect RegisterScreen to actual registration API
    - Replace TODO with actual API call via ViewModel
    - Handle success/error responses
    - Display appropriate messages
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
  - [ ] 7.2 Add domain selection from server
    - Fetch available domains from API
    - Populate dropdown with server domains
    - _Requirements: 1.1, 7.1_

- [ ] 8. Add password change screen
  - [ ] 8.1 Create SettingsScreen with password change
    - Add current password, new password, confirm password fields
    - Implement validation (password length, match)
    - Call changePassword API
    - _Requirements: 12.1, 12.2, 12.3_

- [ ] 9. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Phase 3: Server Configuration and Network Settings

- [ ] 10. Update Android client network configuration
  - [ ] 10.1 Update default server IP to 192.168.1.106
    - Update AccountSetupScreen default values
    - Set SMTP port to 2525, POP3 port to 1100
    - _Requirements: 13.1_
  - [ ] 10.2 Enhance connection test to test both servers
    - Test SMTP connection
    - Test POP3 connection
    - Display specific error for failed server
    - _Requirements: 13.2, 13.3_
  - [ ] 10.3 Write property test for connection settings
    - **Property 14: Configuration storage consistency**
    - **Validates: Requirements 9.1, 9.2, 9.3**

## Phase 4: Email Protocol Enhancement

- [ ] 11. Enhance SMTP server user validation
  - [ ] 11.1 Add user enabled status check in authentication
    - Check is_enabled flag before allowing send
    - Return 535 for disabled accounts
    - _Requirements: 2.3, 6.2_
  - [ ] 11.2 Add domain enabled status check
    - Check domain is_enabled before accepting email
    - Return 550 for disabled domains
    - _Requirements: 7.2_
  - [ ] 11.3 Write property test for domain validation
    - **Property 13: Domain management affects email routing**
    - **Validates: Requirements 7.2, 7.3**

- [ ] 12. Enhance POP3 server user validation
  - [ ] 12.1 Add user enabled status check in authentication
    - Check is_enabled flag during USER/PASS
    - Return -ERR for disabled accounts
    - _Requirements: 2.3, 6.2_
  - [ ] 12.2 Write property test for authentication
    - **Property 4: Authentication with valid credentials succeeds**
    - **Validates: Requirements 2.1, 2.4**

- [ ] 13. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Phase 5: Email Parsing and Round-Trip Testing

- [ ] 14. Add email parsing utilities
  - [ ] 14.1 Create EmailParser class in mail-core
    - Parse RFC 5322 format headers
    - Parse plain text, HTML, multipart content
    - Handle character encoding (UTF-8)
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 15.1_
  - [ ] 14.2 Create EmailSerializer class in mail-core
    - Serialize email to RFC 5322 format
    - Preserve all headers and body
    - Handle special characters
    - _Requirements: 15.2, 15.4_
  - [ ] 14.3 Write property test for email round-trip
    - **Property 17: Email parsing/serialization round-trip**
    - **Validates: Requirements 15.1, 15.2, 15.3**
  - [ ] 14.4 Write property test for character encoding
    - **Property 18: Character encoding preservation**
    - **Validates: Requirements 15.4**
  - [ ] 14.5 Write property test for content parsing
    - **Property 16: Email content parsing preserves structure**
    - **Validates: Requirements 14.1, 14.2, 14.3, 14.4**

- [ ] 15. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Phase 6: Admin Bulk Email Feature

- [ ] 16. Implement bulk email service
  - [ ] 16.1 Create BulkEmailBatch entity and repository
    - Add entity with id, subject, body, counts, status
    - Add repository with CRUD operations
    - _Requirements: 8.1_
  - [ ] 16.2 Create BulkEmailService
    - Implement queueBulkEmail() method
    - Implement getDeliveryStats() method
    - _Requirements: 8.1, 8.2, 8.3_
  - [ ] 16.3 Add bulk email controller endpoint
    - Add POST /admin/bulk-email endpoint
    - Add GET /admin/bulk-email/{id}/stats endpoint
    - _Requirements: 8.1, 8.3_
  - [ ] 16.4 Write property test for bulk email
    - **Property 6: Multi-recipient delivery**
    - **Validates: Requirements 3.3**

## Phase 7: Mail Queue Management

- [ ] 17. Enhance mail queue management
  - [ ] 17.1 Add retry functionality to queue service
    - Implement retry() method to reset retry count
    - Update status to PENDING
    - _Requirements: 11.2_
  - [ ] 17.2 Add cancel functionality to queue service
    - Implement cancel() method
    - Update status to CANCELLED
    - _Requirements: 11.3_
  - [ ] 17.3 Write property test for queue retry
    - **Property 15: Queue retry resets state**
    - **Validates: Requirements 11.2**

## Phase 8: Email Operations Testing

- [ ] 18. Add email operation property tests
  - [ ] 18.1 Write property test for email storage
    - **Property 5: Email sending stores in database**
    - **Validates: Requirements 3.1, 3.2**
  - [ ] 18.2 Write property test for retry mechanism
    - **Property 7: Retry mechanism with exponential backoff**
    - **Validates: Requirements 3.5**
  - [ ] 18.3 Write property test for POP3 LIST
    - **Property 8: POP3 LIST returns correct format**
    - **Validates: Requirements 4.2**
  - [ ] 18.4 Write property test for POP3 RETR
    - **Property 9: POP3 RETR returns complete email**
    - **Validates: Requirements 4.3**
  - [ ] 18.5 Write property test for email deletion
    - **Property 10: Email deletion round-trip**
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.4**

- [ ] 19. Add admin operation property tests
  - [ ] 19.1 Write property test for user management
    - **Property 11: Admin user management operations**
    - **Validates: Requirements 6.1, 6.2, 6.3, 6.4**

- [ ] 20. Final Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
