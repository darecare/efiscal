import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import AppShell from '../components/AppShell'
import { useAuth } from '../contexts/AuthContext'
import { usersApi } from '../services/api'

const MIN_PASSWORD_LENGTH = 6
const EMPTY_PASSWORD_FORM = { newPassword: '', confirmPassword: '' }
const STACK_STYLE = { display: 'flex', flexDirection: 'column', gap: 14 }

export default function Account() {
  const { t } = useTranslation()
  const { user, refreshUser, showNotification } = useAuth()

  const [profile, setProfile] = useState({ fullName: '', email: '' })
  const [profileError, setProfileError] = useState('')
  const [savingProfile, setSavingProfile] = useState(false)

  const [passwordModalOpen, setPasswordModalOpen] = useState(false)
  const [passwordForm, setPasswordForm] = useState(EMPTY_PASSWORD_FORM)
  const [passwordError, setPasswordError] = useState('')
  const [savingPassword, setSavingPassword] = useState(false)

  useEffect(() => {
    setProfile({ fullName: user?.fullName ?? '', email: user?.email ?? '' })
  }, [user?.fullName, user?.email])

  const trimmedName = profile.fullName.trim()
  const trimmedEmail = profile.email.trim()
  const profileChanged = trimmedName !== (user?.fullName ?? '') || trimmedEmail !== (user?.email ?? '')

  async function handleProfileSubmit(e) {
    e.preventDefault()
    setProfileError('')
    if (!trimmedName) {
      setProfileError(t('account.nameRequired'))
      return
    }
    if (!trimmedEmail) {
      setProfileError(t('account.emailRequired'))
      return
    }
    setSavingProfile(true)
    try {
      await usersApi.updateMyProfile({ fullName: trimmedName, email: trimmedEmail })
      await refreshUser()
      showNotification(t('account.profileSaved'), 'success')
    } catch (err) {
      setProfileError(err.response?.data?.message || t('account.profileSaveFailed'))
    } finally {
      setSavingProfile(false)
    }
  }

  function openPasswordModal() {
    setPasswordForm(EMPTY_PASSWORD_FORM)
    setPasswordError('')
    setPasswordModalOpen(true)
  }

  function closePasswordModal() {
    if (savingPassword) return
    setPasswordModalOpen(false)
    setPasswordForm(EMPTY_PASSWORD_FORM)
    setPasswordError('')
  }

  async function handlePasswordSubmit(e) {
    e.preventDefault()
    setPasswordError('')
    const { newPassword, confirmPassword } = passwordForm
    if (!newPassword || !confirmPassword) {
      setPasswordError(t('account.passwordBothRequired'))
      return
    }
    if (newPassword.length < MIN_PASSWORD_LENGTH) {
      setPasswordError(t('account.passwordTooShort', { min: MIN_PASSWORD_LENGTH }))
      return
    }
    if (newPassword !== confirmPassword) {
      setPasswordError(t('account.passwordMismatch'))
      return
    }
    setSavingPassword(true)
    try {
      await usersApi.changeMyPassword(newPassword, confirmPassword)
      setPasswordModalOpen(false)
      setPasswordForm(EMPTY_PASSWORD_FORM)
      showNotification(t('account.passwordChanged'), 'success')
    } catch {
      setPasswordError(t('account.passwordChangeFailed'))
    } finally {
      setSavingPassword(false)
    }
  }

  return (
    <AppShell title={t('account.title')} subtitle={t('account.subtitle')}>
      <section className="card">
        <div className="form-grid">
          <form onSubmit={handleProfileSubmit} style={STACK_STYLE}>
            <h3>{t('account.identity')}</h3>
            <div className="field">
              <label htmlFor="account-full-name">{t('account.nameLabel')} *</label>
              <input
                id="account-full-name"
                className="account-input"
                value={profile.fullName}
                maxLength={255}
                onChange={(e) => setProfile((prev) => ({ ...prev, fullName: e.target.value }))}
              />
            </div>
            <div className="field">
              <label htmlFor="account-email">{t('account.emailLabel')} *</label>
              <input
                id="account-email"
                className="account-input"
                type="email"
                value={profile.email}
                maxLength={255}
                onChange={(e) => setProfile((prev) => ({ ...prev, email: e.target.value }))}
              />
              {trimmedEmail !== (user?.email ?? '') && (
                <small>{t('account.emailChangeHint')}</small>
              )}
            </div>
            <div className="account-readonly">
              <p><strong>{t('account.roleLabel')}:</strong> {user?.roleName}</p>
              <p>
                <strong>{t('account.preferredLanguageLabel')}:</strong>{' '}
                {user?.preferredLanguage
                  ? t(`common.languages.${user.preferredLanguage}`, { defaultValue: user.preferredLanguage })
                  : t('common.noPreference')}
              </p>
            </div>
            {profileError && <p className="error-text">{profileError}</p>}
            <div className="modal-actions">
              <button type="button" className="secondary-button" onClick={openPasswordModal}>
                {t('account.editPassword')}
              </button>
              <button type="submit" className="primary-button" disabled={savingProfile || !profileChanged}>
                {savingProfile ? t('common.saving') : t('common.saveChanges')}
              </button>
            </div>
          </form>
          <div>
            <h3>{t('account.subscription')}</h3>
            <p><strong>{t('account.statusLabel')}:</strong> {user?.subscriptionStatus}</p>
            <p><strong>{t('account.clientLabel')}:</strong> {user?.clientName || t('common.global')}</p>
            <p><strong>{t('account.expiresLabel')}:</strong> {user?.subscriptionExpiresAt || t('account.noExpiry')}</p>
          </div>
        </div>
      </section>

      {passwordModalOpen && (
        <div className="modal-overlay" onClick={closePasswordModal}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{t('account.editPasswordTitle')}</h3>
              <button className="modal-close" onClick={closePasswordModal} aria-label={t('common.close')}>×</button>
            </div>
            <form onSubmit={handlePasswordSubmit} style={STACK_STYLE}>
              <div className="field">
                <label htmlFor="account-new-password">{t('account.newPasswordLabel')} *</label>
                <input
                  id="account-new-password"
                  type="password"
                  autoComplete="new-password"
                  autoFocus
                  value={passwordForm.newPassword}
                  maxLength={100}
                  onChange={(e) => setPasswordForm((prev) => ({ ...prev, newPassword: e.target.value }))}
                />
              </div>
              <div className="field">
                <label htmlFor="account-confirm-password">{t('account.confirmPasswordLabel')} *</label>
                <input
                  id="account-confirm-password"
                  type="password"
                  autoComplete="new-password"
                  value={passwordForm.confirmPassword}
                  maxLength={100}
                  onChange={(e) => setPasswordForm((prev) => ({ ...prev, confirmPassword: e.target.value }))}
                />
              </div>
              {passwordError && <p className="error-text">{passwordError}</p>}
              <div className="modal-actions">
                <button type="button" className="secondary-button" onClick={closePasswordModal} disabled={savingPassword}>
                  {t('common.cancel')}
                </button>
                <button type="submit" className="primary-button" disabled={savingPassword}>
                  {savingPassword ? t('common.saving') : t('common.save')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  )
}
