package com.mytestwork2.fragments

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.mytestwork2.R

class CreditsFragment : Fragment() {

    private lateinit var creditsTextView: TextView
    private lateinit var closeButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_credits, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        creditsTextView = view.findViewById(R.id.creditsTextView)
        closeButton = view.findViewById(R.id.closeButton)

        // Load the attributions text (which contains HTML) from assets
        val htmlText = loadAttributions(requireContext())
        // Use Html.fromHtml to parse the HTML (for API 24+, use FROM_HTML_MODE_LEGACY)
        creditsTextView.text = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY)
        // Enable clickable links
        creditsTextView.movementMethod = LinkMovementMethod.getInstance()

        // Set up the Close button to navigate back.
        closeButton.setOnClickListener {
            findNavController().navigateUp() // or navigate to a specific fragment if needed
        }
    }

    private fun loadAttributions(context: Context): String {
        return context.assets.open("attributions.txt").bufferedReader().use { it.readText() }
    }
}
