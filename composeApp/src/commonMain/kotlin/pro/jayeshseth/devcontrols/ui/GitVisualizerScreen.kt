package pro.jayeshseth.devcontrols.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.ui.text.drawText

// Data model for Git Commits
data class GitCommit(
    val hash: String,
    val parents: List<String>,
    val message: String,
    val seq: Int,
    val isMerge: Boolean = false,
    val isReplanted: Boolean = false
)

// Data model for Git repository state
data class GitState(
    val commits: Map<String, GitCommit>,
    val branches: Map<String, String>, // branchName -> commitHash
    val headRef: String, // "ref:branchName" or "commitHash" (detached HEAD)
    val commitCount: Int
) {
    // Helper to resolve current HEAD hash
    fun resolveHead(): String? {
        return if (headRef.startsWith("ref:")) {
            val branchName = headRef.substring(4)
            branches[branchName]
        } else {
            headRef
        }
    }
}

// Log line types in our console
data class GitConsoleLine(
    val text: String,
    val type: GitLineType
) {
    enum class GitLineType { COMMAND, OUTPUT, ERROR, SYSTEM }
}

// Interactive Git Challenge definition
data class GitChallenge(
    val id: Int,
    val title: String,
    val description: String,
    val setupInstructions: String,
    val goalDescription: String,
    val initialSetup: () -> GitState,
    val checkSuccess: (GitState) -> Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitVisualizerScreen(onBack: () -> Unit) {
    // Predefined Git challenges
    val challenges = remember {
        listOf(
            GitChallenge(
                id = 0,
                title = "Playground / Sandbox",
                description = "Free play mode. Learn and experiment with standard Git workflows at your own pace.",
                setupInstructions = "Try committing, branching, checking out, merging, and rebasing. Switch to other tabs for challenges.",
                goalDescription = "No goal - explore freely!",
                initialSetup = {
                    val initialCommits = mapOf(
                        "c0" to GitCommit("c0", emptyList(), "Initial commit", 0),
                        "c1" to GitCommit("c1", listOf("c0"), "Add README.md", 1),
                        "c2" to GitCommit("c2", listOf("c1"), "Implement layout", 2)
                    )
                    GitState(
                        commits = initialCommits,
                        branches = mapOf("main" to "c2"),
                        headRef = "ref:main",
                        commitCount = 3
                    )
                },
                checkSuccess = { false }
            ),
            GitChallenge(
                id = 1,
                title = "1. Commit & Branch",
                description = "Learn how branches isolate work. Create a new branch, checkout to it, and commit changes.",
                setupInstructions = "1. Create a branch named 'dev'\n2. Checkout to 'dev'\n3. Make at least 2 commits on it",
                goalDescription = "Create a 'dev' branch, checkout to it, and make 2 new commits.",
                initialSetup = {
                    val initialCommits = mapOf(
                        "c0" to GitCommit("c0", emptyList(), "Initial commit", 0),
                        "c1" to GitCommit("c1", listOf("c0"), "Add README.md", 1)
                    )
                    GitState(
                        commits = initialCommits,
                        branches = mapOf("main" to "c1"),
                        headRef = "ref:main",
                        commitCount = 2
                    )
                },
                checkSuccess = { state ->
                    val devTip = state.branches["dev"]
                    if (devTip == null || state.headRef != "ref:dev") return@GitChallenge false
                    // Verify dev has at least 2 commits ahead of the starting point c1
                    val ancestors = getAncestors(devTip, state.commits)
                    ancestors.size >= 4 // c0, c1 + at least 2 more
                }
            ),
            GitChallenge(
                id = 2,
                title = "2. Fast-Forward Merge",
                description = "Merge a branch when main hasn't moved since they diverged. Git simply slides the pointer forward.",
                setupInstructions = "1. Checkout the 'main' branch\n2. Run 'git merge feature'\nVerify that main simply moves without creating a new merge commit.",
                goalDescription = "Merge 'feature' into 'main'. main and feature should point to the same commit.",
                initialSetup = {
                    val initialCommits = mapOf(
                        "c0" to GitCommit("c0", emptyList(), "Initial commit", 0),
                        "c1" to GitCommit("c1", listOf("c0"), "Add README.md", 1),
                        "c2" to GitCommit("c2", listOf("c1"), "Implement buttons", 2),
                        "c3" to GitCommit("c3", listOf("c2"), "Add analytics tracking", 3)
                    )
                    GitState(
                        commits = initialCommits,
                        branches = mapOf("main" to "c2", "feature" to "c3"),
                        headRef = "ref:main",
                        commitCount = 4
                    )
                },
                checkSuccess = { state ->
                    state.branches["main"] == state.branches["feature"] && state.headRef == "ref:main"
                }
            ),
            GitChallenge(
                id = 3,
                title = "3. Three-Way Merge",
                description = "Merge diverged branches. Git merges changes and creates a new 'Merge Commit' with two parent references.",
                setupInstructions = "Merge 'feature' into 'main' while both branches have diverged commits.",
                goalDescription = "Checkout main, then run 'git merge feature' to create a merge commit (a commit with 2 parents).",
                initialSetup = {
                    val initialCommits = mapOf(
                        "c0" to GitCommit("c0", emptyList(), "Initial commit", 0),
                        "c1" to GitCommit("c1", listOf("c0"), "Add README.md", 1),
                        "c2" to GitCommit("c2", listOf("c1"), "Create base framework", 2),
                        "c3" to GitCommit("c3", listOf("c2"), "Main feature implementation", 3), // main tip
                        "c4" to GitCommit("c4", listOf("c2"), "Feature layout refinements", 4)  // feature tip
                    )
                    GitState(
                        commits = initialCommits,
                        branches = mapOf("main" to "c3", "feature" to "c4"),
                        headRef = "ref:main",
                        commitCount = 5
                    )
                },
                checkSuccess = { state ->
                    val mainTip = state.branches["main"] ?: return@GitChallenge false
                    val commit = state.commits[mainTip] ?: return@GitChallenge false
                    state.headRef == "ref:main" && commit.isMerge && commit.parents.size == 2
                }
            ),
            GitChallenge(
                id = 4,
                title = "4. Rebase to Linearize",
                description = "Move commits from one branch on top of another. This avoids merge commits and keeps history perfectly linear.",
                setupInstructions = "1. Make sure you are on 'feature'\n2. Rebase 'feature' onto 'main'\n3. Checkout 'main'\n4. Merge 'feature' to fast-forward main",
                goalDescription = "Rebase 'feature' onto 'main' and merge it, so main and feature point to the same commit in a linear line.",
                initialSetup = {
                    val initialCommits = mapOf(
                        "c0" to GitCommit("c0", emptyList(), "Initial commit", 0),
                        "c1" to GitCommit("c1", listOf("c0"), "Add README.md", 1),
                        "c2" to GitCommit("c2", listOf("c1"), "Diverge base", 2),
                        "c3" to GitCommit("c3", listOf("c2"), "Update docs on main", 3), // main
                        "c4" to GitCommit("c4", listOf("c2"), "Work on feature screen", 4) // feature
                    )
                    GitState(
                        commits = initialCommits,
                        branches = mapOf("main" to "c3", "feature" to "c4"),
                        headRef = "ref:feature",
                        commitCount = 5
                    )
                },
                checkSuccess = { state ->
                    val mainTip = state.branches["main"]
                    val featureTip = state.branches["feature"]
                    if (mainTip == null || featureTip == null || mainTip != featureTip) return@GitChallenge false
                    
                    // Verify history from featureTip is linear
                    var curr: String? = featureTip
                    var pathHasMerge = false
                    val visited = mutableSetOf<String>()
                    while (curr != null && curr != "c0") {
                        if (curr in visited) break
                        visited.add(curr)
                        val commit = state.commits[curr] ?: break
                        if (commit.parents.size > 1) {
                            pathHasMerge = true
                            break
                        }
                        curr = commit.parents.firstOrNull()
                    }
                    !pathHasMerge && state.headRef == "ref:main"
                }
            )
        )
    }

    var selectedChallengeIndex by remember { mutableStateOf(0) }
    val currentChallenge = challenges[selectedChallengeIndex]

    var gitState by remember(selectedChallengeIndex) { mutableStateOf(currentChallenge.initialSetup()) }
    val consoleLines = remember(selectedChallengeIndex) {
        mutableStateListOf<GitConsoleLine>().apply {
            add(GitConsoleLine("Welcome to the Git Branching & Rebase Interactive Lab!", GitConsoleLine.GitLineType.SYSTEM))
            add(GitConsoleLine("Type commands in the console (e.g. 'git commit', 'git branch dev', 'git checkout dev', 'git merge main', 'git rebase main').", GitConsoleLine.GitLineType.SYSTEM))
            add(GitConsoleLine("Or use the Quick Actions panel below for shortcut execution.", GitConsoleLine.GitLineType.SYSTEM))
            add(GitConsoleLine("Active challenge: ${currentChallenge.title}", GitConsoleLine.GitLineType.SYSTEM))
            add(GitConsoleLine("----------------------------------------------------------------", GitConsoleLine.GitLineType.SYSTEM))
        }
    }
    
    var commandInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Check success on state changes
    val isSuccess = remember(gitState) {
        currentChallenge.checkSuccess(gitState)
    }

    fun logOutput(output: String) {
        consoleLines.add(GitConsoleLine(output, GitConsoleLine.GitLineType.OUTPUT))
        scope.launch {
            if (consoleLines.size > 0) {
                listState.animateScrollToItem(consoleLines.size - 1)
            }
        }
    }

    fun logError(err: String) {
        consoleLines.add(GitConsoleLine(err, GitConsoleLine.GitLineType.ERROR))
        scope.launch {
            if (consoleLines.size > 0) {
                listState.animateScrollToItem(consoleLines.size - 1)
            }
        }
    }

    fun runGitCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return
        
        consoleLines.add(GitConsoleLine("$ $trimmed", GitConsoleLine.GitLineType.COMMAND))
        
        if (!trimmed.startsWith("git ")) {
            logError("Error: Command must start with 'git'")
            return
        }
        val parts = trimmed.substring(4).split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (parts.isEmpty()) {
            logError("Error: Incomplete git command")
            return
        }
        
        val action = parts[0]
        when (action) {
            "commit" -> {
                var message = "Commit"
                val mIndex = parts.indexOf("-m")
                if (mIndex != -1 && mIndex + 1 < parts.size) {
                    var rawMsg = parts.subList(mIndex + 1, parts.size).joinToString(" ")
                    if (rawMsg.startsWith("\"") && rawMsg.endsWith("\"")) {
                        rawMsg = rawMsg.substring(1, rawMsg.length - 1)
                    } else if (rawMsg.startsWith("'") && rawMsg.endsWith("'")) {
                        rawMsg = rawMsg.substring(1, rawMsg.length - 1)
                    }
                    message = rawMsg
                }
                
                val headHash = gitState.resolveHead()
                if (headHash == null) {
                    logError("Error: HEAD is not pointing to any valid commit")
                    return
                }
                
                val newHash = "c${gitState.commitCount}"
                val newCommit = GitCommit(
                    hash = newHash,
                    parents = listOf(headHash),
                    message = message,
                    seq = gitState.commitCount
                )
                
                val updatedCommits = gitState.commits + (newHash to newCommit)
                val updatedBranches = if (gitState.headRef.startsWith("ref:")) {
                    val branchName = gitState.headRef.substring(4)
                    gitState.branches + (branchName to newHash)
                } else {
                    gitState.branches
                }
                val updatedHeadRef = if (gitState.headRef.startsWith("ref:")) {
                    gitState.headRef
                } else {
                    newHash 
                }
                
                gitState = gitState.copy(
                    commits = updatedCommits,
                    branches = updatedBranches,
                    headRef = updatedHeadRef,
                    commitCount = gitState.commitCount + 1
                )
                logOutput("[$newHash] Commit created: \"$message\"")
                if (updatedHeadRef == newHash && !gitState.headRef.startsWith("ref:")) {
                    logOutput("Note: Committed in detached HEAD state. HEAD is now at $newHash.")
                }
            }
            
            "branch" -> {
                if (parts.size < 2) {
                    logError("Error: Usage: git branch <branch-name>")
                    return
                }
                val branchName = parts[1]
                if (gitState.branches.containsKey(branchName)) {
                    logError("Error: Branch '$branchName' already exists")
                    return
                }
                
                val headHash = gitState.resolveHead()
                if (headHash == null) {
                    logError("Error: Cannot create branch because HEAD is empty")
                    return
                }
                
                val updatedBranches = gitState.branches + (branchName to headHash)
                gitState = gitState.copy(branches = updatedBranches)
                logOutput("Branch '$branchName' created pointing to $headHash")
            }
            
            "checkout" -> {
                if (parts.size < 2) {
                    logError("Error: Usage: git checkout <branch-or-commit>")
                    return
                }
                
                if (parts[1] == "-b") {
                    if (parts.size < 3) {
                        logError("Error: Usage: git checkout -b <branch-name>")
                        return
                    }
                    val branchName = parts[2]
                    if (gitState.branches.containsKey(branchName)) {
                        logError("Error: Branch '$branchName' already exists")
                        return
                    }
                    val headHash = gitState.resolveHead()
                    if (headHash == null) {
                        logError("Error: HEAD is empty")
                        return
                    }
                    
                    val updatedBranches = gitState.branches + (branchName to headHash)
                    gitState = gitState.copy(
                        branches = updatedBranches,
                        headRef = "ref:$branchName"
                    )
                    logOutput("Switched to a new branch '$branchName'")
                } else {
                    val target = parts[1]
                    if (gitState.branches.containsKey(target)) {
                        gitState = gitState.copy(headRef = "ref:$target")
                        logOutput("Switched to branch '$target'")
                    } else if (gitState.commits.containsKey(target)) {
                        gitState = gitState.copy(headRef = target)
                        logOutput("Note: Switched to detached HEAD at '$target'. Committing will not update any branch.")
                    } else {
                        logError("Error: Pathspec '$target' did not match any branch or commit hash in repository.")
                    }
                }
            }
            
            "merge" -> {
                if (parts.size < 2) {
                    logError("Error: Usage: git merge <branch-name>")
                    return
                }
                val targetBranch = parts[1]
                val targetHash = gitState.branches[targetBranch]
                if (targetHash == null) {
                    logError("Error: Branch '$targetBranch' does not exist")
                    return
                }
                
                val headHash = gitState.resolveHead()
                if (headHash == null) {
                    logError("Error: HEAD is empty")
                    return
                }
                
                if (targetHash == headHash) {
                    logOutput("Already up to date.")
                    return
                }
                
                val headAncestors = getAncestors(headHash, gitState.commits)
                if (targetHash in headAncestors) {
                    logOutput("Already up to date.")
                    return
                }
                
                val targetAncestors = getAncestors(targetHash, gitState.commits)
                if (headHash in targetAncestors) {
                    val updatedBranches = if (gitState.headRef.startsWith("ref:")) {
                        val activeBranch = gitState.headRef.substring(4)
                        gitState.branches + (activeBranch to targetHash)
                    } else {
                        gitState.branches
                    }
                    val updatedHeadRef = if (gitState.headRef.startsWith("ref:")) {
                        gitState.headRef
                    } else {
                        targetHash
                    }
                    
                    gitState = gitState.copy(
                        branches = updatedBranches,
                        headRef = updatedHeadRef
                    )
                    logOutput("Fast-forward merge: moved pointer to $targetHash")
                    return
                }
                
                val newHash = "c${gitState.commitCount}"
                val currentBranchName = if (gitState.headRef.startsWith("ref:")) gitState.headRef.substring(4) else "detached"
                val mergeCommit = GitCommit(
                    hash = newHash,
                    parents = listOf(headHash, targetHash),
                    message = "Merge branch '$targetBranch' into $currentBranchName",
                    seq = gitState.commitCount,
                    isMerge = true
                )
                
                val updatedCommits = gitState.commits + (newHash to mergeCommit)
                val updatedBranches = if (gitState.headRef.startsWith("ref:")) {
                    val activeBranch = gitState.headRef.substring(4)
                    gitState.branches + (activeBranch to newHash)
                } else {
                    gitState.branches
                }
                val updatedHeadRef = if (gitState.headRef.startsWith("ref:")) {
                    gitState.headRef
                } else {
                    newHash
                }
                
                gitState = gitState.copy(
                    commits = updatedCommits,
                    branches = updatedBranches,
                    headRef = updatedHeadRef,
                    commitCount = gitState.commitCount + 1
                )
                logOutput("Merge commit created: $newHash (Parents: $headHash, $targetHash)")
            }
            
            "rebase" -> {
                if (parts.size < 2) {
                    logError("Error: Usage: git rebase <branch-name>")
                    return
                }
                val targetBranch = parts[1]
                val targetHash = gitState.branches[targetBranch]
                if (targetHash == null) {
                    logError("Error: Branch '$targetBranch' does not exist")
                    return
                }
                
                val headHash = gitState.resolveHead()
                if (headHash == null) {
                    logError("Error: HEAD is empty")
                    return
                }
                
                if (targetHash == headHash) {
                    logOutput("Already up to date.")
                    return
                }
                
                val lca = findLCA(headHash, targetHash, gitState.commits)
                if (lca == null) {
                    logError("Error: No common ancestor found between HEAD and target branch.")
                    return
                }
                
                if (lca == headHash) {
                    val updatedBranches = if (gitState.headRef.startsWith("ref:")) {
                        val activeBranch = gitState.headRef.substring(4)
                        gitState.branches + (activeBranch to targetHash)
                    } else {
                        gitState.branches
                    }
                    val updatedHeadRef = if (gitState.headRef.startsWith("ref:")) {
                        gitState.headRef
                    } else {
                        targetHash
                    }
                    gitState = gitState.copy(
                        branches = updatedBranches,
                        headRef = updatedHeadRef
                    )
                    logOutput("Rebase fast-forward: Moved tip to $targetHash")
                    return
                }
                
                if (lca == targetHash) {
                    logOutput("Current branch is already on top of '$targetBranch'.")
                    return
                }
                
                val hAncestors = getAncestors(headHash, gitState.commits).toSet()
                val tAncestors = getAncestors(targetHash, gitState.commits).toSet()
                val commitsToReplay = hAncestors.subtract(tAncestors)
                    .sortedBy { gitState.commits[it]?.seq ?: 0 } 
                
                var currentTip: String = targetHash
                val tempCommits = gitState.commits.toMutableMap()
                var currentCount = gitState.commitCount
                
                commitsToReplay.forEach { originalHash ->
                    val orig = gitState.commits[originalHash]
                    if (orig != null && !orig.isMerge) {
                        val newHash = "c$currentCount"
                        val replanted = GitCommit(
                            hash = newHash,
                            parents = listOf(currentTip),
                            message = "${orig.message} (rebase)",
                            seq = currentCount,
                            isReplanted = true
                        )
                        tempCommits[newHash] = replanted
                        currentTip = newHash
                        currentCount++
                    }
                }
                
                val updatedBranches = if (gitState.headRef.startsWith("ref:")) {
                    val activeBranch = gitState.headRef.substring(4)
                    gitState.branches + (activeBranch to currentTip)
                } else {
                    gitState.branches
                }
                val updatedHeadRef = if (gitState.headRef.startsWith("ref:")) {
                    gitState.headRef
                } else {
                    currentTip
                }
                
                gitState = gitState.copy(
                    commits = tempCommits,
                    branches = updatedBranches,
                    headRef = updatedHeadRef,
                    commitCount = currentCount
                )
                logOutput("Rebased commits from ancestor $lca onto $targetHash.")
                logOutput("New head tip: $currentTip")
            }
            
            "reset" -> {
                if (parts.size >= 3 && parts[1] == "--hard") {
                    val target = parts[2]
                    var targetHash: String? = null
                    if (gitState.branches.containsKey(target)) {
                        targetHash = gitState.branches[target]
                    } else if (gitState.commits.containsKey(target)) {
                        targetHash = target
                    }
                    
                    if (targetHash == null) {
                        logError("Error: Reference '$target' not found")
                        return
                    }
                    
                    val updatedBranches = if (gitState.headRef.startsWith("ref:")) {
                        val activeBranch = gitState.headRef.substring(4)
                        gitState.branches + (activeBranch to targetHash)
                    } else {
                        gitState.branches
                    }
                    val updatedHeadRef = if (gitState.headRef.startsWith("ref:")) {
                        gitState.headRef
                    } else {
                        targetHash
                    }
                    
                    gitState = gitState.copy(
                        branches = updatedBranches,
                        headRef = updatedHeadRef
                    )
                    logOutput("Reset tip to $targetHash")
                } else {
                    logError("Error: Currently only 'git reset --hard <target>' is supported.")
                }
            }
            
            "help" -> {
                logOutput("Supported commands:")
                logOutput("  git commit [-m \"message\"] : Make a new commit")
                logOutput("  git branch <name>          : Create a branch pointer")
                logOutput("  git checkout <name>        : Switch to branch or specific hash")
                logOutput("  git checkout -b <name>     : Create and checkout a new branch")
                logOutput("  git merge <branch>         : Merge branch into current branch")
                logOutput("  git rebase <branch>        : Rebase current branch onto target")
                logOutput("  git reset --hard <ref>     : Reset pointer to commit/branch")
            }
            
            else -> {
                logError("Error: Unsupported git command '$action'. Type 'git help' for list.")
            }
        }
        
        scope.launch {
            if (consoleLines.size > 0) {
                listState.animateScrollToItem(consoleLines.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Git Branch & Rebase Laboratory", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { 
                        Text("< Back", color = Color.White, fontWeight = FontWeight.Bold) 
                    }
                },
                actions = {
                    TextButton(onClick = {
                        gitState = currentChallenge.initialSetup()
                        consoleLines.clear()
                        consoleLines.add(GitConsoleLine("State reset to initial challenge configuration.", GitConsoleLine.GitLineType.SYSTEM))
                    }) {
                        Text("Reset Graph", color = Color(0xFFEF4444))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E24),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF121214))
        ) {
            val isWideScreen = maxWidth >= 850.dp
            
            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Pane: Terminal and Quick Actions
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .padding(12.dp)
                    ) {
                        TerminalConsole(
                            consoleLines = consoleLines,
                            commandInput = commandInput,
                            onCommandInputChange = { commandInput = it },
                            onSendCommand = {
                                runGitCommand(commandInput)
                                commandInput = ""
                            },
                            listState = listState,
                            modifier = Modifier.weight(0.65f)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        QuickActionsPanel(
                            onCommand = { runGitCommand(it) },
                            gitState = gitState,
                            modifier = Modifier.weight(0.35f)
                        )
                    }
                    
                    // Vertical divider
                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color(0xFF2D2D35)))
                    
                    // Right Pane: Canvas Visualization and Challenge details
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                    ) {
                        // Graph visualizer
                        Box(
                            modifier = Modifier
                                .weight(0.6f)
                                .fillMaxWidth()
                                .background(Color(0xFF151518))
                        ) {
                            GitGraphCanvas(
                                state = gitState,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        HorizontalDivider(color = Color(0xFF2D2D35))
                        
                        // Lesson / Challenge panel
                        Box(
                            modifier = Modifier
                                .weight(0.4f)
                                .fillMaxWidth()
                                .background(Color(0xFF18181C))
                        ) {
                            ChallengePanel(
                                challenges = challenges,
                                selectedIndex = selectedChallengeIndex,
                                onSelectChallenge = { selectedChallengeIndex = it },
                                challenge = currentChallenge,
                                isSuccess = isSuccess
                            )
                        }
                    }
                }
            } else {
                // Portrait layout
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top part: Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.35f)
                            .background(Color(0xFF151518))
                    ) {
                        GitGraphCanvas(
                            state = gitState,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    HorizontalDivider(color = Color(0xFF2D2D35))
                    
                    // Middle part: Challenge status
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.2f)
                            .background(Color(0xFF18181C))
                    ) {
                        ChallengePanel(
                            challenges = challenges,
                            selectedIndex = selectedChallengeIndex,
                            onSelectChallenge = { selectedChallengeIndex = it },
                            challenge = currentChallenge,
                            isSuccess = isSuccess
                        )
                    }
                    
                    HorizontalDivider(color = Color(0xFF2D2D35))
                    
                    // Bottom part: Console and Quick Buttons
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.45f)
                            .padding(8.dp)
                    ) {
                        TerminalConsole(
                            consoleLines = consoleLines,
                            commandInput = commandInput,
                            onCommandInputChange = { commandInput = it },
                            onSendCommand = {
                                runGitCommand(commandInput)
                                commandInput = ""
                            },
                            listState = listState,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        QuickActionsPanel(
                            onCommand = { runGitCommand(it) },
                            gitState = gitState,
                            modifier = Modifier.height(130.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalConsole(
    consoleLines: List<GitConsoleLine>,
    commandInput: String,
    onCommandInputChange: (String) -> Unit,
    onSendCommand: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
        border = BorderStroke(1.dp, Color(0xFF26262D))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B1B1F))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF5F56)))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFFBD2E)))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF27C93F)))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "git-bash",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            // Log output
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(consoleLines) { line ->
                    val color = when (line.type) {
                        GitConsoleLine.GitLineType.COMMAND -> Color(0xFF00FF66)
                        GitConsoleLine.GitLineType.OUTPUT -> Color(0xFFECEFF1)
                        GitConsoleLine.GitLineType.ERROR -> Color(0xFFFF3366)
                        GitConsoleLine.GitLineType.SYSTEM -> Color(0xFF00E5FF)
                    }
                    Text(
                        text = line.text,
                        color = color,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
            
            HorizontalDivider(color = Color(0xFF26262D))
            
            // Console Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$ git ",
                    color = Color(0xFF00FF66),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                BasicTextField(
                    value = commandInput,
                    onValueChange = onCommandInputChange,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp)
                        .onKeyEvent {
                            if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) {
                                onSendCommand()
                                true
                            } else {
                                false
                            }
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                        autoCorrect = false
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { onSendCommand() }
                    )
                )
                
                TextButton(
                    onClick = onSendCommand,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Run", color = Color(0xFF00FF66), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun QuickActionsPanel(
    onCommand: (String) -> Unit,
    gitState: GitState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        border = BorderStroke(1.dp, Color(0xFF2D2D35))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = "Quick Actions Panel",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { onCommand("git commit") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Text("Commit", fontSize = 12.sp, color = Color.White)
                    }
                    
                    OutlinedButton(
                        onClick = { onCommand("git branch feature") },
                        border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Text("Branch feature", fontSize = 11.sp, color = Color(0xFF60A5FA))
                    }
                    
                    OutlinedButton(
                        onClick = { onCommand("git branch dev") },
                        border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Text("Branch dev", fontSize = 11.sp, color = Color(0xFF60A5FA))
                    }
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            val activeBranch = if (gitState.headRef == "ref:main") "feature" else "main"
                            onCommand("git checkout $activeBranch")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        val activeBranch = if (gitState.headRef == "ref:main") "feature" else "main"
                        Text("Checkout $activeBranch", fontSize = 11.sp, color = Color.White)
                    }
                    
                    Button(
                        onClick = {
                            val target = if (gitState.headRef == "ref:main") "feature" else "main"
                            onCommand("git merge $target")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        val target = if (gitState.headRef == "ref:main") "feature" else "main"
                        Text("Merge $target", fontSize = 11.sp, color = Color.White)
                    }
                    
                    Button(
                        onClick = {
                            val target = if (gitState.headRef == "ref:feature") "main" else "feature"
                            onCommand("git rebase $target")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        val target = if (gitState.headRef == "ref:feature") "main" else "feature"
                        Text("Rebase $target", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun GitGraphCanvas(
    state: GitState,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    
    // Compute positions dynamically
    val positions = remember(state) {
        val sortedCommits = state.commits.keys.sortedWith(compareBy { 
            it.substring(1).toIntOrNull() ?: 0 
        })
        
        val lanes = mutableMapOf<String, Int>() 
        val cols = mutableMapOf<String, Int>() 
        
        if (sortedCommits.isNotEmpty()) {
            lanes[sortedCommits[0]] = 0
            cols[sortedCommits[0]] = 0
        }
        
        val parentToChildren = mutableMapOf<String, MutableList<String>>()
        state.commits.forEach { (hash, commit) ->
            commit.parents.forEach { parent ->
                parentToChildren.getOrPut(parent) { mutableListOf() }.add(hash)
            }
        }
        
        val activeLanes = mutableSetOf<Int>()
        activeLanes.add(0)
        
        for (i in 1 until sortedCommits.size) {
            val hash = sortedCommits[i]
            val commit = state.commits[hash] ?: continue
            val primaryParent = commit.parents.firstOrNull() ?: continue
            
            val maxParentCol = commit.parents.map { cols[it] ?: 0 }.maxOrNull() ?: 0
            cols[hash] = maxParentCol + 1
            
            val siblings = parentToChildren[primaryParent] ?: emptyList()
            val myIndex = siblings.indexOf(hash)
            
            if (myIndex <= 0) {
                lanes[hash] = lanes[primaryParent] ?: 0
            } else {
                var candidate = (lanes[primaryParent] ?: 0) + myIndex
                if (myIndex % 2 == 1) {
                    candidate = (lanes[primaryParent] ?: 0) - ((myIndex + 1) / 2)
                }
                while (activeLanes.contains(candidate)) {
                    candidate += if (candidate >= 0) 1 else -1
                }
                lanes[hash] = candidate
                activeLanes.add(candidate)
            }
        }
        
        val xSpacing = 110f
        val ySpacing = 70f
        val yCenter = 180f
        
        sortedCommits.associateWith { hash ->
            val col = cols[hash] ?: 0
            val lane = lanes[hash] ?: 0
            Offset(70f + col * xSpacing, yCenter + lane * ySpacing)
        }
    }
    
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    
    Box(
        modifier = modifier
            .horizontalScroll(horizontalScrollState)
            .verticalScroll(verticalScrollState)
            .width(800.dp)
            .height(400.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = 18f
            
            // Draw connection lines
            state.commits.forEach { (hash, commit) ->
                val childPos = positions[hash] ?: return@forEach
                
                commit.parents.forEach { parent ->
                    val parentPos = positions[parent] ?: return@forEach
                    
                    // Draw curved line using Cubic Bezier
                    val path = Path().apply {
                        moveTo(childPos.x, childPos.y)
                        cubicTo(
                            childPos.x - 40f, childPos.y,
                            parentPos.x + 40f, parentPos.y,
                            parentPos.x, parentPos.y
                        )
                    }
                    
                    val lineColor = if (commit.isReplanted) Color(0xFFF59E0B) else Color(0xFF4B5563)
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(
                            width = 3f, 
                            cap = StrokeCap.Round,
                            pathEffect = if (commit.isReplanted) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                        )
                    )
                    
                    // Add subtle indicator dot pointing back to parent
                    val direction = (parentPos - childPos)
                    if (direction.getDistance() > 0.1f) {
                        val arrowX = parentPos.x + 24f
                        val arrowY = parentPos.y
                        drawCircle(
                            color = lineColor,
                            radius = 4f,
                            center = Offset(arrowX, arrowY)
                        )
                    }
                }
            }
            
            // Draw commit circles
            state.commits.forEach { (hash, commit) ->
                val center = positions[hash] ?: return@forEach
                
                val baseColor = when {
                    commit.isMerge -> Color(0xFF8B5CF6)
                    commit.isReplanted -> Color(0xFFF59E0B)
                    else -> Color(0xFF3B82F6)
                }
                
                drawCircle(
                    color = baseColor.copy(alpha = 0.25f),
                    radius = radius + 8f,
                    center = center
                )
                
                drawCircle(
                    color = baseColor,
                    radius = radius,
                    center = center
                )
                
                drawCircle(
                    color = Color.White,
                    radius = 5f,
                    center = center
                )
                
                drawText(
                    textMeasurer = textMeasurer,
                    text = commit.hash,
                    topLeft = Offset(center.x - 14f, center.y + radius + 2f),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            // Draw Branch references & HEAD pointer
            val branchGroups = state.branches.entries.groupBy { it.value } // commitHash -> List<BranchName>
            
            branchGroups.forEach { (hash, entries) ->
                val center = positions[hash] ?: return@forEach
                
                var tagOffset = 0f
                entries.forEach { entry ->
                    val branchName = entry.key
                    val isCurrentBranch = state.headRef == "ref:$branchName"
                    val tagBg = if (isCurrentBranch) Color(0xFF10B981) else Color(0xFF374151)
                    val tagText = branchName
                    
                    val boxWidth = 65f
                    val boxHeight = 22f
                    val boxX = center.x - (boxWidth / 2f)
                    val boxY = center.y - 45f - tagOffset
                    
                    drawRoundRect(
                        color = tagBg,
                        topLeft = Offset(boxX, boxY),
                        size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                    
                    drawText(
                        textMeasurer = textMeasurer,
                        text = tagText,
                        topLeft = Offset(boxX + 6f, boxY + 2f),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    )
                    
                    if (isCurrentBranch) {
                        drawRoundRect(
                            color = Color(0xFFEF4444),
                            topLeft = Offset(boxX, boxY - 22f),
                            size = androidx.compose.ui.geometry.Size(42f, 18f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                        )
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "HEAD",
                            topLeft = Offset(boxX + 6f, boxY - 20f),
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                    
                    tagOffset += boxHeight + 4f
                }
            }
            
            // Draw HEAD directly on node if detached
            if (!state.headRef.startsWith("ref:")) {
                val headHash = state.headRef
                val center = positions[headHash] ?: return@Canvas
                
                val boxWidth = 80f
                val boxHeight = 22f
                val boxX = center.x - (boxWidth / 2f)
                val boxY = center.y - 48f
                
                drawRoundRect(
                    color = Color(0xFFEF4444),
                    topLeft = Offset(boxX, boxY),
                    size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
                
                drawText(
                    textMeasurer = textMeasurer,
                    text = "HEAD (det)",
                    topLeft = Offset(boxX + 6f, boxY + 3f),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}

@Composable
fun ChallengePanel(
    challenges: List<GitChallenge>,
    selectedIndex: Int,
    onSelectChallenge: (Int) -> Unit,
    challenge: GitChallenge,
    isSuccess: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth().height(36.dp)
        ) {
            challenges.forEachIndexed { index, ch ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { onSelectChallenge(index) },
                    text = { Text(ch.title, fontSize = 11.sp) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Goal:",
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = challenge.goalDescription,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "Instructions:",
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = challenge.setupInstructions,
                    color = Color(0xFFD1D5DB),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 16.sp
                )
            }
            
            if (selectedIndex != 0) { 
                Column(
                    modifier = Modifier
                        .width(110.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isSuccess) {
                        Text(
                            text = "✓",
                            color = Color(0xFF10B981),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "COMPLETED!",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "ℹ",
                            color = Color(0xFF3B82F6),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "IN PROGRESS",
                            color = Color(0xFF3B82F6),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Helper: Get list of ancestors for a commit recursively
fun getAncestors(hash: String, commits: Map<String, GitCommit>): List<String> {
    val result = mutableListOf<String>()
    val queue = mutableListOf(hash)
    val visited = mutableSetOf<String>()
    while (queue.isNotEmpty()) {
        val curr = queue.removeAt(0)
        if (curr !in visited) {
            visited.add(curr)
            result.add(curr)
            commits[curr]?.parents?.forEach { queue.add(it) }
        }
    }
    return result
}

// Helper: Find Lowest Common Ancestor (LCA)
fun findLCA(h: String, t: String, commits: Map<String, GitCommit>): String? {
    val hAncestors = getAncestors(h, commits).toSet()
    val tAncestorsOrdered = getAncestors(t, commits)
    
    val visited = mutableSetOf<String>()
    val queue = mutableListOf(h)
    while (queue.isNotEmpty()) {
        val curr = queue.removeAt(0)
        if (curr in tAncestorsOrdered) {
            return curr
        }
        if (curr !in visited) {
            visited.add(curr)
            commits[curr]?.parents?.forEach { queue.add(it) }
        }
    }
    return null
}
