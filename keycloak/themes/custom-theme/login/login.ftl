<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        <div class="custom-header">
            <h1>Bienvenue sur Enclume-Numérique</h1>
            <p>Connectez-vous à votre compte</p>
        </div>
    <#elseif section = "form">
        <script>
        (function() {
            document.addEventListener('DOMContentLoaded', function() {
                const passwordInput = document.getElementById('password');
                const passwordToggle = document.getElementById('password-toggle');
                const iconShow = document.getElementById('password-toggle-icon-show');
                const iconHide = document.getElementById('password-toggle-icon-hide');
                
                if (passwordInput && passwordToggle && iconShow && iconHide) {
                    passwordToggle.addEventListener('click', function(e) {
                        e.preventDefault();
                        e.stopPropagation();
                        
                        const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
                        passwordInput.setAttribute('type', type);
                        
                        if (type === 'text') {
                            iconShow.classList.add('hidden');
                            iconHide.classList.remove('hidden');
                            passwordToggle.setAttribute('aria-label', 'Masquer le mot de passe');
                        } else {
                            iconShow.classList.remove('hidden');
                            iconHide.classList.add('hidden');
                            passwordToggle.setAttribute('aria-label', 'Afficher le mot de passe');
                        }
                    });
                }
            });
        })();
        </script>
        <div class="custom-login-container">
            <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                <div class="form-group">
                    <label for="username" class="custom-label">
                        <#if !realm.loginWithEmailAllowed>
                            Nom d'utilisateur
                        <#elseif !realm.registrationEmailAsUsername>
                            Nom d'utilisateur ou email
                        <#else>
                            Email
                        </#if>
                    </label>
                    <input tabindex="1" id="username" class="custom-input" name="username" 
                           value="${(login.username!'')}" type="text" autofocus autocomplete="off"
                           aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                           placeholder="Entrez votre nom d'utilisateur ou email" />
                </div>

                <div class="form-group">
                    <label for="password" class="custom-label">Mot de passe</label>
                    <div class="password-input-wrapper">
                        <input tabindex="2" id="password" class="custom-input password-input" name="password" type="password" autocomplete="off"
                               aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                               placeholder="Entrez votre mot de passe" />
                        <button type="button" class="password-toggle" id="password-toggle" aria-label="Afficher le mot de passe">
                            <svg class="password-toggle-icon" id="password-toggle-icon-show" width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                                <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                            </svg>
                            <svg class="password-toggle-icon hidden" id="password-toggle-icon-hide" width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                                <line x1="1" y1="1" x2="23" y2="23" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                            </svg>
                        </button>
                    </div>
                </div>

                <div class="form-options">
                    <#if realm.rememberMe && !usernameHidden??>
                        <div class="checkbox">
                            <label>
                                <#if login.rememberMe??>
                                    <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" checked> Se souvenir de moi
                                <#else>
                                    <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"> Se souvenir de moi
                                </#if>
                            </label>
                        </div>
                    </#if>
                    <div class="forgot-password">
                        <#if realm.resetPasswordAllowed>
                            <span><a tabindex="5" href="${url.loginResetCredentialsUrl}">Mot de passe oublié ?</a></span>
                        </#if>
                    </div>
                </div>

                <div id="kc-form-buttons" class="form-buttons">
                    <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                    <input tabindex="4" class="custom-button primary" name="login" id="kc-login" type="submit" value="Se connecter"/>
                </div>
            </form>
        </div>
    </#if>
</@layout.registrationLayout>

