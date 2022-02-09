package com.sagisu.vault.ui.login.fragments;

import androidx.databinding.ObservableField;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.sagisu.vault.R;
import com.sagisu.vault.network.VaultAPIError;
import com.sagisu.vault.network.VaultResult;
import com.sagisu.vault.repository.NetworkRepository;
import com.sagisu.vault.utils.OtpTypeDescriptor;

import java.util.Objects;

public class VaultLoginViewModel extends ViewModel {
    public interface PageType {
        String SignUp = "Sign-up";
        String Login = "login";
        String ForgotPasswordPhone = "forgot_password_phone";
        String Otp = "otp";
        String Password = "password";
        String AUTO_LOGIN_EXTERNAL_APP = "auto_login_external_app";
    }

    public interface ViewType {
        String SignUp = "Sign-up";
        String Login = "login";
        String ForgotPassword = "forgot_password";
        String AUTO_LOGIN_EXTERNAL_APP = "auto_login_external_app";
    }

    public MutableLiveData<Integer> pagePos = new MutableLiveData<>(1);
    public MutableLiveData<String> page = new MutableLiveData<>(PageType.Login);
    public MutableLiveData<String> viewType = new MutableLiveData<>();
    public MutableLiveData<String> otpStatus = new MutableLiveData<>();
    public MutableLiveData<String> otpNumber = new MutableLiveData<>();
    public MutableLiveData<User> consumer = new MutableLiveData<>(new User());
    public MediatorLiveData<String> loginStatus = new MediatorLiveData<>();
    public MutableLiveData<String> toastMsg = new MutableLiveData<>();
    public LoginResponse loginRes;
    private final MutableLiveData<String> loadingObservable = new MutableLiveData<>(null);
    private final MutableLiveData<LoginFormError> formError = new MutableLiveData<>(new LoginFormError());
    private ObservableField<Boolean> validateFields = new ObservableField<>(true);

    public void init(String viewType) {
        setViewType(viewType);
    }

    public void onNextClicked() {
        pagePos.setValue(2);
        // pagePos.setValue(pagePos.getValue() + 1);
    }

    public void generateOtp() {
        if (!valid()) {
            toastMsg.setValue("Please fill the mandatory fields");
            return;
        }
        otpStatus.setValue(OtpTypeDescriptor.GENERATE);
    }

    public void verifyOtp() {
        LoginFormError tmpFormError = new LoginFormError();
        if (otpNumber.getValue() == null || otpNumber.getValue().isEmpty()) {
            tmpFormError.setOtpError(R.string.required_field_error);
            formError.setValue(tmpFormError);
            return;
        }

        otpStatus.setValue(OtpTypeDescriptor.VERIFY);
    }

    public void resendOtp() {
        otpStatus.setValue(OtpTypeDescriptor.RESEND);
    }

    public void skipProfileUpdate() {
        loginStatus.setValue("DoLogin");
    }

    public boolean valid() {
        if (viewType.getValue().equals(ViewType.AUTO_LOGIN_EXTERNAL_APP)) return true;
        boolean flag = true;
        LoginFormError tmpFormError = new LoginFormError();
        switch (page.getValue()) {
            case PageType.Login:
                if (consumer == null || consumer.getValue() == null) {
                    tmpFormError.setToastMsg("Please fill all mandatory fields");
                    return false;
                }
                if (consumer.getValue().getPhone() == null || consumer.getValue().getPhone().isEmpty()) {
                    tmpFormError.setPhoneError(R.string.required_field_error);
                    flag = false;
                }
                if (consumer.getValue().getPassword() == null || consumer.getValue().getPassword().isEmpty()) {
                    tmpFormError.setPasswordError(R.string.required_field_error);
                    flag = false;
                }
                break;
            case PageType.ForgotPasswordPhone:
                if (consumer.getValue().getPhone() == null || consumer.getValue().getPhone().isEmpty()) {
                    tmpFormError.setPhoneError(R.string.required_field_error);
                    flag = false;
                }
                break;
            case PageType.SignUp:
                if (consumer == null || consumer.getValue() == null) {
                    tmpFormError.setToastMsg("Please fill all mandatory fields");
                    return false;
                }
                if (consumer.getValue().getPhone() == null || consumer.getValue().getPhone().isEmpty()) {
                    tmpFormError.setPhoneError(R.string.required_field_error);
                    flag = false;
                }
                break;
            case PageType.Password:
                if (consumer == null || consumer.getValue() == null) {
                    tmpFormError.setToastMsg("Please fill all mandatory fields");
                    return false;
                }
                if (consumer.getValue().getPassword() == null || consumer.getValue().getPassword().isEmpty()) {
                    tmpFormError.setPasswordError(R.string.required_field_error);
                    flag = false;
                } else if (consumer.getValue().getConfirmPassword() == null || consumer.getValue().getConfirmPassword().isEmpty()) {
                    tmpFormError.setConfirmPasswordError(R.string.required_field_error);
                    flag = false;
                } else if (!consumer.getValue().getPassword().equals(consumer.getValue().getConfirmPassword())) {
                    tmpFormError.setConfirmPasswordError(R.string.password_mismatch);
                    flag = false;
                }
                break;
            default:
                if (pagePos.getValue() == 2 && (otpNumber.getValue() == null || otpNumber.getValue().isEmpty())) {
                    tmpFormError.setOtpError(R.string.required_field_error);
                    flag = false;
                }
        }
        formError.setValue(tmpFormError);

        return flag;
    }

    public void updateCustomer() {
        /*LiveData<Result<User>> liveData = NetworkRepo.updateCustomer(consumer.getValue(),consumer.getValue().getId(), errorHandler);
        loginStatus.addSource(liveData, new Observer<Result<User>>() {
            @Override
            public void onChanged(Result<User> agentResult) {
                if (agentResult instanceof Result.Success) {
                    loginStatus.setValue("DoLogin");
                } else if (agentResult instanceof Result.Error) {
                    APIError apiError = ((Result.Error) agentResult).getError();
                }
            }
        });*/
    }

    public void login() {
        if (!valid()) return;
        loadingObservable.setValue("Logging in");
        LiveData<VaultResult<LoginResponse>> liveData = NetworkRepository.getInstance().login(consumer.getValue());
        loginStatus.addSource(liveData, new Observer<VaultResult<LoginResponse>>() {
            @Override
            public void onChanged(VaultResult<LoginResponse> agentResult) {
                loadingObservable.setValue(null);
                if (agentResult instanceof VaultResult.Success) {
                    loginRes = ((VaultResult.Success<LoginResponse>) agentResult).getData();
                    //loginStatus.setValue("LoggedIn");
                    generateOtp();
                } else if (agentResult instanceof VaultResult.Error) {
                    VaultAPIError vaultApiError = ((VaultResult.Error) agentResult).getError();
                    toastMsg.setValue(vaultApiError.message());
                }
            }
        });
    }

    public void onPasswordScreenSubmit() {
        switch (Objects.requireNonNull(viewType.getValue())) {
            case ViewType.ForgotPassword:
                resetPassword();
                break;
            case ViewType.SignUp:
                signUp();
                break;
        }
    }

    public void signUp() {
        if (!valid()) return;
        loadingObservable.setValue("Signing Up");
        LiveData<VaultResult<LoginResponse>> liveData = NetworkRepository.getInstance().signUp(consumer.getValue());
        loginStatus.addSource(liveData, new Observer<VaultResult<LoginResponse>>() {
            @Override
            public void onChanged(VaultResult<LoginResponse> agentResult) {
                loadingObservable.setValue(null);
                if (agentResult instanceof VaultResult.Success) {
                    loginRes = ((VaultResult.Success<LoginResponse>) agentResult).getData();
                    loginStatus.setValue("SignedUp");
                    //setViewType(ViewType.Login);

                } else if (agentResult instanceof VaultResult.Error) {
                    VaultAPIError vaultApiError = ((VaultResult.Error) agentResult).getError();
                    if (vaultApiError.status() == 403) {
                        toastMsg.setValue("Phone is already registered");
                    }
                }
            }
        });
    }

    public void resetPassword() {
        if (!valid()) return;
        loadingObservable.setValue("Resetting password");
        LiveData<VaultResult<LoginResponse>> liveData = NetworkRepository.getInstance().forgotPassword(consumer.getValue());
        loginStatus.addSource(liveData, new Observer<VaultResult<LoginResponse>>() {
            @Override
            public void onChanged(VaultResult<LoginResponse> agentResult) {
                loadingObservable.setValue(null);
                if (agentResult instanceof VaultResult.Success) {
                    setViewType(ViewType.Login);

                } else if (agentResult instanceof VaultResult.Error) {
                    VaultAPIError vaultApiError = ((VaultResult.Error) agentResult).getError();
                    if (vaultApiError.status() == 403) {
                        toastMsg.setValue(vaultApiError.message());
                    }
                }
            }
        });
    }

    public void userExists() {
        if (!valid()) {
            toastMsg.setValue("Please fill the mandatory fields");
            return;
        }

        loadingObservable.setValue("Checking if user exist");
        LiveData<VaultResult<LoginResponse>> liveData = NetworkRepository.getInstance().getProfile(consumer.getValue().getPhone());
        loginStatus.addSource(liveData, new Observer<VaultResult<LoginResponse>>() {
            @Override
            public void onChanged(VaultResult<LoginResponse> agentResult) {
                loadingObservable.setValue(null);
                if (agentResult instanceof VaultResult.Success) {
                    loginRes = ((VaultResult.Success<LoginResponse>) agentResult).getData();
                    switch (page.getValue()) {
                        case PageType.Login:
                        case PageType.ForgotPasswordPhone:
                            if (loginRes.getUser() == null) {
                                toastMsg.setValue("Phone number is not registered");
                            } else
                                generateOtp();
                            break;
                        case PageType.SignUp:
                            if (loginRes.getUser() == null) generateOtp();
                            else
                                toastMsg.setValue("Phone number is already registered");
                            break;
                        case PageType.AUTO_LOGIN_EXTERNAL_APP:
                            if (loginRes.getUser() == null) signUp();
                            else
                                login();
                            break;
                    }

                } else if (agentResult instanceof VaultResult.Error) {
                    VaultAPIError vaultApiError = ((VaultResult.Error) agentResult).getError();
                    toastMsg.setValue(vaultApiError.message());
                }
            }
        });
    }

    public void autoLoginExternalApp(String phone, String password) {
        User user = new User();
        user.setPhone(phone);
        user.setPassword(password);
        consumer.setValue(user);
        userExists();
    }

    public void setViewType(String viewType) {
        this.viewType.setValue(viewType);

        switch (viewType) {
            case ViewType.Login:
                page.setValue(PageType.Login);
                break;
            case ViewType.SignUp:
                page.setValue(PageType.SignUp);
                break;
            case ViewType.ForgotPassword:
                page.setValue(PageType.ForgotPasswordPhone);
                break;
            case ViewType.AUTO_LOGIN_EXTERNAL_APP:
                page.setValue(PageType.AUTO_LOGIN_EXTERNAL_APP);
                break;
        }
    }

    public void skipLogin() {
        loginStatus.setValue("SkipLogin");
    }

    public LoginResponse getLoginRes() {
        return loginRes;
    }

    public MutableLiveData<String> getLoadingObservable() {
        return loadingObservable;
    }

    public void setFcmToken(String token) {
        consumer.getValue().setFcmToken(token);
    }

    public void setPage(String pageType) {
        page.setValue(pageType);
    }

    public MutableLiveData<LoginFormError> getFormError() {
        return formError;
    }

    public ObservableField<Boolean> getValidateFields() {
        return validateFields;
    }
}