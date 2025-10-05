package za.ac.iie.TallyUp.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import za.ac.iie.TallyUp.R

class StartTutorialFragment : Fragment(R.layout.fragment_start_tutorial) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val getStartedButton = view.findViewById<Button>(R.id.get_started_button)

        getStartedButton.setOnClickListener {
            // Navigate to Question 1 instead of Sign Up
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Question1TutorialFragment())
                .addToBackStack("start_to_question1")
                .commit()
        }
    }
}