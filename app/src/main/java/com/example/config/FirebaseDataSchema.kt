package com.example.config

/**
 * FIREBASE / FIRESTORE SCHEMA SPECIFICATION & SECURITY RULES
 * ==========================================================
 *
 * 1. USER ROLES HIERARCHY:
 * ----------------------------------------------------------
 * - "god" (Admin / Global Owner):
 *     Email: nosovsergej886@gmail.com
 *     Permissions: Full read/write access across all collections, all service centers,
 *                  global user management, invite token generation, unlimited technician quotas.
 * - "service_admin" (Service Center Admin):
 *     Permissions: Read/write for their own service center entity, technicians list,
 *                  invitation generation for new technicians, service center chat logs & tickets.
 * - "master" / "technician" (Technician / Component Repair Engineer):
 *     Permissions: Read/write own chat diagnoses, view shared knowledge base & schematics,
 *                  participate in assigned service center boards.
 * - "viewer" (Trainee / Observer):
 *     Permissions: Read-only access to knowledge base and diagnostic history.
 *
 *
 * 2. FIRESTORE COLLECTIONS STRUCTURE:
 * ----------------------------------------------------------
 *
 * /users/{userId}
 *   - id: string
 *   - email: string
 *   - username: string
 *   - name: string
 *   - role: string ("god" | "service_admin" | "master" | "viewer")
 *   - serviceCenterId: string | null
 *   - serviceCenterName: string | null
 *   - city: string | null
 *   - maxMastersLimit: number (default: 5, god: 9999)
 *   - authProvider: string ("google" | "password")
 *   - avatarUrl: string | null
 *   - createdAt: timestamp
 *
 * /service_centers/{centerId}
 *   - id: string
 *   - name: string
 *   - city: string
 *   - adminEmail: string
 *   - adminName: string
 *   - phoneOrTelegram: string
 *   - maxMastersLimit: number
 *   - isApproved: boolean
 *   - createdAt: timestamp
 *
 * /invitations/{inviteCode}
 *   - id: string
 *   - inviteCode: string (e.g. "INV-TECH-7892")
 *   - serviceCenterId: string
 *   - serviceCenterName: string
 *   - createdByAdminEmail: string
 *   - createdByAdminName: string
 *   - targetRole: string ("master" | "viewer")
 *   - createdAt: timestamp
 *   - expiresAt: timestamp (default: 7 days)
 *   - isUsed: boolean
 *   - usedByUserId: string | null
 *   - usedByUserName: string | null
 *   - usedByUserEmail: string | null
 *   - usedAt: timestamp | null
 *
 * /chats/{chatId}
 *   - id: string
 *   - title: string
 *   - userId: string
 *   - serviceCenterId: string
 *   - lastMessage: string
 *   - lastMessageTime: timestamp
 *   - isPinned: boolean
 *   - messages: collection (/chats/{chatId}/messages/{messageId})
 *       - role: "user" | "ai"
 *       - text: string
 *       - imageUrl: string | null
 *       - guideJson: string | null
 *       - timestamp: timestamp
 *       - isSaved: boolean
 *
 * /knowledge_base/{entryId}
 *   - id: string
 *   - brand: string
 *   - model: string
 *   - problem: string
 *   - guideDataJson: string
 *   - addedBy: string
 *   - isSchematic: boolean
 *   - addedDate: timestamp
 *
 *
 * 3. FIRESTORE SECURITY RULES (Production Definition):
 * ----------------------------------------------------------
 * ```javascript
 * rules_version = '2';
 * service cloud.firestore {
 *   match /databases/{database}/documents {
 *
 *     function isAuth() {
 *       return request.auth != null;
 *     }
 *
 *     function isGodMode() {
 *       return isAuth() && (
 *         request.auth.token.email.lower() == 'nosovsergej886@gmail.com' ||
 *         request.auth.token.role == 'god'
 *       );
 *     }
 *
 *     function isServiceAdmin(centerId) {
 *       return isAuth() && (
 *         isGodMode() ||
 *         (request.auth.token.role == 'service_admin' && request.auth.token.serviceCenterId == centerId)
 *       );
 *     }
 *
 *     match /users/{userId} {
 *       allow read: if isAuth();
 *       allow write: if isGodMode() || (isAuth() && request.auth.uid == userId);
 *     }
 *
 *     match /service_centers/{centerId} {
 *       allow read: if isAuth();
 *       allow write: if isGodMode() || isServiceAdmin(centerId);
 *     }
 *
 *     match /invitations/{inviteCode} {
 *       allow read: if isAuth();
 *       allow create: if isGodMode() || request.auth.token.role == 'service_admin';
 *       allow update: if isAuth(); // For redemption
 *       allow delete: if isGodMode() || request.auth.token.role == 'service_admin';
 *     }
 *
 *     match /chats/{chatId}/{document=**} {
 *       allow read, write: if isAuth();
 *     }
 *
 *     match /knowledge_base/{entryId} {
 *       allow read: if isAuth();
 *       allow write: if isAuth();
 *     }
 *   }
 * }
 * ```
 */
object FirebaseDataSchema {
    const val GOD_MODE_EMAIL = "nosovsergej886@gmail.com"
    const val ROLE_GOD = "god"
    const val ROLE_SERVICE_ADMIN = "service_admin"
    const val ROLE_TECHNICIAN = "master"
    const val ROLE_VIEWER = "viewer"

    const val COLLECTION_USERS = "users"
    const val COLLECTION_SERVICE_CENTERS = "service_centers"
    const val COLLECTION_INVITATIONS = "invitations"
    const val COLLECTION_CHATS = "chats"
    const val COLLECTION_MESSAGES = "messages"
    const val COLLECTION_KNOWLEDGE = "knowledge_base"
}
