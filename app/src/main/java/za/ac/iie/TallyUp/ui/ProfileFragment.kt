package za.ac.iie.TallyUp.ui

import za.ac.iie.TallyUp.R
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.data.DatabaseProvider
import za.ac.iie.TallyUp.databinding.FragmentProfileBinding


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        val email = prefs.getString("loggedInEmail", null)

        if (email != null) {
            val db = DatabaseProvider.getDatabase(requireContext())

            lifecycleScope.launch {
                val user = db.userDao().getUserByEmail(email)

                if (user != null) {
                    val fullName = "${user.firstName} ${user.lastName}"
                    binding.userName.text = fullName
                    binding.firstNameInput.setText(user.firstName)
                    binding.lastNameInput.setText(user.lastName)
                } else {
                    binding.userName.text = getString(R.string.user_not_found)
                }
            }
        } else {
            binding.userName.text = getString(R.string.no_user_logged_in)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}