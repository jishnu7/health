package xyz.jishnu.health.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.jishnu.health.data.local.Profile
import xyz.jishnu.health.data.model.Sex
import xyz.jishnu.health.data.repo.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: ProfileRepository,
) : ViewModel() {
    val profile: StateFlow<Profile> = repo.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Profile.Empty,
    )

    fun setSex(sex: Sex?) = viewModelScope.launch { repo.setSex(sex) }
    fun setHeightCm(cm: Double?) = viewModelScope.launch { repo.setHeightCm(cm) }
    fun setDateOfBirth(iso: String?) = viewModelScope.launch { repo.setDateOfBirth(iso) }
}
