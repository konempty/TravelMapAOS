package kim.hanbin.gpstracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kim.hanbin.gpstracker.databinding.ActivityLoginBinding


class LoginActivity : AppCompatActivity() {

    private var mBinding: ActivityLoginBinding? = null
    private val binding get() = mBinding!!
    private lateinit var mAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    private lateinit var buttonFacebookLogin: LoginButton
    private lateinit var popup: ProgressPopup


    var googleLoginResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
                setResult()
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                setResult()
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(resources.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        mAuth = Firebase.auth

        callbackManager = CallbackManager.Factory.create()

        buttonFacebookLogin = LoginButton(this)
        buttonFacebookLogin.setPermissions("email", "public_profile")
        buttonFacebookLogin.registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
                hideProgress()
            }

            override fun onError(error: FacebookException) {

                Toast.makeText(
                    this@LoginActivity,
                    "문제가 발생했습니다. 잠시후 다시 시도해주세요.",
                    Toast.LENGTH_LONG
                ).show()
                setResult()
            }
        })
        binding.googleLogin.setOnClickListener {
            showProgress()
            googleLoginResult.launch(googleSignInClient.signInIntent)
        }
        binding.appleLogin.setOnClickListener {
            showProgress()
            val provider = OAuthProvider.newBuilder("apple.com")
            provider.scopes =
                mutableListOf("email", "name")// Localize the Apple authentication screen in French.
            provider.addCustomParameter("locale", "ko")

            (mAuth.pendingAuthResult
                ?: mAuth.startActivityForSignInWithProvider(
                    this,
                    provider.build()
                )).addOnSuccessListener { authResult ->
                setResult()
            }.addOnFailureListener { e ->
                setResult()
            }
        }

        binding.facebookLogin.setOnClickListener {
            showProgress()
            buttonFacebookLogin.performClick()
        }

        binding.twitterLogin.setOnClickListener {
            showProgress()
            val provider = OAuthProvider.newBuilder("twitter.com")

            provider.addCustomParameter("locale", "ko")

            (mAuth.pendingAuthResult
                ?: mAuth.startActivityForSignInWithProvider(
                    this,
                    provider.build()
                )).addOnSuccessListener { authResult ->
                setResult()
            }.addOnFailureListener { e ->
                setResult()
            }
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }


    private fun firebaseAuthWithGoogle(idToken: String) {
        //Firebase.auth.signOut()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                setResult()
            }
    }


    private fun handleFacebookAccessToken(token: AccessToken) {

        val credential = FacebookAuthProvider.getCredential(token.token)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                setResult()
            }
    }

    fun setResult() {
        hideProgress()
        if (mAuth.currentUser == null) {
            setResult(RESULT_CANCELED)
        } else {
            setResult(RESULT_OK)
        }
        finish()
    }

    fun showProgress() {
        popup = ProgressPopup(this)
        try {
            popup.show()
        } catch (e: IllegalArgumentException) {
            Log.e(this.javaClass.simpleName, "showProgress IllegalArgumentException")
        } catch (e: RuntimeException) {
            Log.e(this.javaClass.simpleName, "showProgress RuntimeException")
        } catch (e: Exception) {
            Log.e(this.javaClass.simpleName, "showProgress Exception")
        }
    }


    fun hideProgress() {
        try {
            popup.dismiss()
        } catch (e: IllegalArgumentException) {
            Log.e(this.javaClass.simpleName, "hideProgress IllegalArgumentException")
        } catch (e: RuntimeException) {
            Log.e(this.javaClass.simpleName, "hideProgress RuntimeException")
        } catch (e: Exception) {
            Log.e(this.javaClass.simpleName, "hideProgress Exception")
        }
    }
}