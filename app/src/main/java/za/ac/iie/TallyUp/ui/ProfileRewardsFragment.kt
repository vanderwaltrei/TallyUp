@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import za.ac.iie.TallyUp.R
import za.ac.iie.TallyUp.databinding.FragmentProfileRewardsBinding
import za.ac.iie.TallyUp.models.Achievement
import za.ac.iie.TallyUp.utils.AchievementManager
import java.text.SimpleDateFormat
import java.util.*

class ProfileRewardsFragment : Fragment() {

    private var _binding: FragmentProfileRewardsBinding? = null
    private val binding get() = _binding!!
    private lateinit var achievementAdapter: AchievementAdapter

    companion object {
        private const val TAG = "ProfileRewardsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "🎯 ProfileRewardsFragment onViewCreated")
        loadAchievements()
    }

    @SuppressLint("SetTextI18n")
    private fun loadAchievements() {
        val userId = getCurrentUserId()
        Log.d(TAG, "📊 Loading achievements for userId: $userId")

        if (userId.isEmpty() || userId == "default") {
            Log.e(TAG, "❌ Invalid userId: '$userId' - cannot load achievements")
            showErrorState()
            return
        }

        lifecycleScope.launch {
            try {
                // Get achievement stats
                Log.d(TAG, "📈 Fetching achievement stats...")
                val stats = AchievementManager.getAchievementStats(requireContext(), userId)
                Log.d(TAG, "✅ Stats received: ${stats.unlockedCount}/${stats.totalCount} unlocked, ${stats.totalCoinsEarned} coins")

                // Update summary cards
                binding.onTrackCount.text = "${stats.unlockedCount}\nUnlocked"
                binding.watchCount.text = "${stats.totalCount - stats.unlockedCount}\nLocked"
                binding.criticalCount.text = "${stats.completionPercentage}%\nComplete"
                binding.overCount.text = "${stats.totalCoinsEarned}\nCoins Earned"

                // Get all achievements
                Log.d(TAG, "📋 Fetching all achievements...")
                val allAchievements = AchievementManager.getAllAchievements(requireContext(), userId)
                Log.d(TAG, "✅ Retrieved ${allAchievements.size} total achievements")

                if (allAchievements.isEmpty()) {
                    Log.w(TAG, "⚠️ No achievements found - initializing now...")
                    // Try to initialize achievements if they don't exist
                    AchievementManager.initializeAchievements(requireContext(), userId)

                    // Retry loading
                    val retryAchievements = AchievementManager.getAllAchievements(requireContext(), userId)
                    if (retryAchievements.isEmpty()) {
                        Log.e(TAG, "❌ Still no achievements after initialization")
                        showEmptyState()
                        return@launch
                    } else {
                        Log.d(TAG, "✅ Successfully loaded ${retryAchievements.size} achievements after initialization")
                        displayAchievements(retryAchievements, userId)
                    }
                } else {
                    displayAchievements(allAchievements, userId)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading achievements: ${e.message}", e)
                e.printStackTrace()
                showErrorState()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun displayAchievements(allAchievements: List<Achievement>, userId: String) {
        // Get recently unlocked
        Log.d(TAG, "🎖️ Fetching recent achievements...")
        val recentlyUnlocked = AchievementManager.getRecentlyUnlocked(requireContext(), userId, 3)
        Log.d(TAG, "✅ Retrieved ${recentlyUnlocked.size} recent achievements")

        // Setup recent achievements section
        binding.recentAchievementsContainer.removeAllViews()

        if (recentlyUnlocked.isNotEmpty()) {
            Log.d(TAG, "📌 Displaying ${recentlyUnlocked.size} recent achievements")
            recentlyUnlocked.forEach { achievement ->
                val itemView = layoutInflater.inflate(
                    R.layout.item_achievement_summary,
                    binding.recentAchievementsContainer,
                    false
                )

                itemView.findViewById<android.widget.ImageView>(R.id.achievementIcon)
                    .setImageResource(achievement.iconResId)
                itemView.findViewById<android.widget.TextView>(R.id.achievementName).text = achievement.name
                itemView.findViewById<android.widget.TextView>(R.id.achievementDescription).text =
                    formatUnlockDate(achievement.unlockedAt)
                itemView.findViewById<android.widget.TextView>(R.id.achievementReward).text =
                    "${achievement.coinReward}"

                binding.recentAchievementsContainer.addView(itemView)
                Log.d(TAG, "  ✅ Added recent achievement: ${achievement.name}")
            }
        } else {
            Log.d(TAG, "ℹ️ No recent achievements to display")
            val emptyView = layoutInflater.inflate(
                android.R.layout.simple_list_item_1,
                binding.recentAchievementsContainer,
                false
            ) as android.widget.TextView
            emptyView.text = "No achievements unlocked yet"
            emptyView.setTextColor(resources.getColor(R.color.muted_foreground, null))
            emptyView.textAlignment = View.TEXT_ALIGNMENT_CENTER
            emptyView.setPadding(16, 16, 16, 16)
            binding.recentAchievementsContainer.addView(emptyView)
        }

        // Setup all achievements RecyclerView
        Log.d(TAG, "📋 Setting up achievements RecyclerView with ${allAchievements.size} items")
        achievementAdapter = AchievementAdapter(allAchievements)
        binding.allAchievementsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.allAchievementsRecycler.adapter = achievementAdapter

        Log.d(TAG, "✅ All achievements displayed successfully")
    }

    private fun showEmptyState() {
        binding.onTrackCount.text = "0\nUnlocked"
        binding.watchCount.text = "0\nLocked"
        binding.criticalCount.text = "0%\nComplete"
        binding.overCount.text = "0\nCoins Earned"

        binding.recentAchievementsContainer.removeAllViews()
        val emptyView = layoutInflater.inflate(
            android.R.layout.simple_list_item_1,
            binding.recentAchievementsContainer,
            false
        ) as android.widget.TextView
        emptyView.text = "No achievements available. Please restart the app."
        emptyView.setTextColor(resources.getColor(R.color.error, null))
        emptyView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        emptyView.setPadding(16, 16, 16, 16)
        binding.recentAchievementsContainer.addView(emptyView)
    }

    private fun showErrorState() {
        binding.onTrackCount.text = "Error\nLoading"
        binding.watchCount.text = "Error\nLoading"
        binding.criticalCount.text = "Error\nLoading"
        binding.overCount.text = "Error\nLoading"
    }

    private fun formatUnlockDate(timestamp: Long): String {
        if (timestamp == 0L) return "Locked"
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return "Unlocked: ${sdf.format(Date(timestamp))}"
    }

    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("userId", null)

        Log.d(TAG, "🔍 Retrieved userId from SharedPreferences: '$userId'")

        // Fallback to loggedInEmail if userId is not set
        if (userId.isNullOrEmpty()) {
            val email = prefs.getString("loggedInEmail", "default")
            Log.d(TAG, "⚠️ userId not found, using email as fallback: '$email'")
            return email ?: "default"
        }

        return userId
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🔄 onResume - reloading achievements")
        loadAchievements()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Achievement Adapter for RecyclerView
class AchievementAdapter(
    private val achievements: List<Achievement>
) : RecyclerView.Adapter<AchievementAdapter.ViewHolder>() {

    companion object {
        private const val TAG = "AchievementAdapter"
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: android.widget.ImageView = itemView.findViewById(R.id.achievementIcon)
        val name: android.widget.TextView = itemView.findViewById(R.id.achievementName)
        val description: android.widget.TextView = itemView.findViewById(R.id.achievementDescription)
        val progress: android.widget.ProgressBar = itemView.findViewById(R.id.achievementProgress)
        val progressText: android.widget.TextView = itemView.findViewById(R.id.achievementProgressText)
        val reward: android.widget.TextView = itemView.findViewById(R.id.achievementReward)
        val badge: android.widget.TextView = itemView.findViewById(R.id.achievementBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement_full, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val achievement = achievements[position]

        Log.d(TAG, "Binding achievement: ${achievement.name} (unlocked: ${achievement.isUnlocked}, progress: ${achievement.progress}/${achievement.maxProgress})")

        holder.icon.setImageResource(achievement.iconResId)
        holder.name.text = if (achievement.isUnlocked) achievement.name else "???"
        holder.description.text = if (achievement.isUnlocked) achievement.description else "Locked achievement"
        holder.reward.text = "${achievement.coinReward}"

        // Progress bar
        val progressPercent = if (achievement.maxProgress > 0) {
            (achievement.progress * 100) / achievement.maxProgress
        } else {
            0
        }
        holder.progress.progress = progressPercent
        holder.progressText.text = "${achievement.progress}/${achievement.maxProgress}"

        // Badge for rarity
        holder.badge.text = achievement.rarity.name
        holder.badge.setBackgroundColor(when (achievement.rarity) {
            za.ac.iie.TallyUp.models.AchievementRarity.COMMON -> 0xFF9E9E9E.toInt()
            za.ac.iie.TallyUp.models.AchievementRarity.RARE -> 0xFF2196F3.toInt()
            za.ac.iie.TallyUp.models.AchievementRarity.EPIC -> 0xFF9C27B0.toInt()
            za.ac.iie.TallyUp.models.AchievementRarity.LEGENDARY -> 0xFFFF9800.toInt()
        })

        // Gray out if locked
        holder.itemView.alpha = if (achievement.isUnlocked) 1.0f else 0.5f
    }

    override fun getItemCount() = achievements.size
}