@file:Suppress("PackageName")

package za.ac.iie.TallyUp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
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
        loadAchievements()
    }

    @SuppressLint("SetTextI18n")
    private fun loadAchievements() {
        val userId = getCurrentUserId()

        lifecycleScope.launch {
            try {
                // Get achievement stats
                val stats = AchievementManager.getAchievementStats(requireContext(), userId)

                // Update summary cards
                binding.onTrackCount.text = "${stats.unlockedCount}\nUnlocked"
                binding.watchCount.text = "${stats.totalCount - stats.unlockedCount}\nLocked"
                binding.criticalCount.text = "${stats.completionPercentage}%\nComplete"
                binding.overCount.text = "${stats.totalCoinsEarned}\nCoins Earned"

                // Get all achievements
                val allAchievements = AchievementManager.getAllAchievements(requireContext(), userId)

                // Get recently unlocked
                val recentlyUnlocked = AchievementManager.getRecentlyUnlocked(requireContext(), userId, 3)

                // Setup recent achievements section
                if (recentlyUnlocked.isNotEmpty()) {
                    binding.recentAchievementsContainer.removeAllViews()
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
                    }
                }

                // Setup all achievements RecyclerView
                achievementAdapter = AchievementAdapter(allAchievements)
                binding.allAchievementsRecycler.layoutManager = LinearLayoutManager(requireContext())
                binding.allAchievementsRecycler.adapter = achievementAdapter

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun formatUnlockDate(timestamp: Long): String {
        if (timestamp == 0L) return "Locked"
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return "Unlocked: ${sdf.format(Date(timestamp))}"
    }

    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("TallyUpPrefs", Context.MODE_PRIVATE)
        return prefs.getString("userId", "") ?: "default"
    }

    override fun onResume() {
        super.onResume()
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