param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$SuperAdminEmail = "superadmin",
    [string]$SuperAdminPassword = "superadmin123",
    [string]$SuperAdminToken = "",
    [string]$OutputDirectory = ".\test-reports",
    [int]$TimeoutSec = 30
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-JsonString {
    param(
        [Parameter(Mandatory = $false)]
        $Value,
        [int]$Depth = 20
    )

    if ($null -eq $Value) {
        return ""
    }

    try {
        return ($Value | ConvertTo-Json -Depth $Depth)
    }
    catch {
        return [string]$Value
    }
}

function Get-TrimmedText {
    param(
        [string]$Text,
        [int]$MaxLength = 4000
    )

    if ([string]::IsNullOrEmpty($Text)) {
        return ""
    }

    if ($Text.Length -le $MaxLength) {
        return $Text
    }

    return $Text.Substring(0, $MaxLength) + "`n...[truncated]"
}

function Escape-MarkdownTableCell {
    param([string]$Text)

    if ($null -eq $Text) {
        return ""
    }

    return $Text.Replace("|", "\\|")
}

function Invoke-ApiRequest {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Uri,
        [hashtable]$Headers = @{},
        $Body = $null,
        [int]$TimeoutSec = 30
    )

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

    try {
        $params = @{
            Uri             = $Uri
            Method          = $Method
            Headers         = $Headers
            TimeoutSec      = $TimeoutSec
            UseBasicParsing = $true
        }

        if ($null -ne $Body) {
            $params["ContentType"] = "application/json"
            $params["Body"] = Get-JsonString -Value $Body -Depth 30
        }

        $response = Invoke-WebRequest @params
        $stopwatch.Stop()

        return [pscustomobject]@{
            StatusCode   = [int]$response.StatusCode
            BodyText     = [string]$response.Content
            ErrorMessage = ""
            DurationMs   = [int]$stopwatch.ElapsedMilliseconds
        }
    }
    catch {
        $stopwatch.Stop()

        $statusCode = 0
        $bodyText = ""
        $errorMessage = $_.Exception.Message

        if ($_.Exception.Response -ne $null) {
            try {
                $statusCode = [int]$_.Exception.Response.StatusCode.value__
            }
            catch {
                $statusCode = 0
            }

            try {
                $stream = $_.Exception.Response.GetResponseStream()
                if ($stream -ne $null) {
                    $reader = New-Object System.IO.StreamReader($stream)
                    $bodyText = $reader.ReadToEnd()
                    $reader.Close()
                }
            }
            catch {
                if ([string]::IsNullOrWhiteSpace($bodyText)) {
                    $bodyText = ""
                }
            }
        }

        if ([string]::IsNullOrWhiteSpace($bodyText)) {
            try {
                if ($null -ne $_.ErrorDetails -and -not [string]::IsNullOrWhiteSpace($_.ErrorDetails.Message)) {
                    $bodyText = [string]$_.ErrorDetails.Message
                }
            }
            catch {
                $bodyText = [string]$bodyText
            }
        }

        return [pscustomobject]@{
            StatusCode   = $statusCode
            BodyText     = [string]$bodyText
            ErrorMessage = [string]$errorMessage
            DurationMs   = [int]$stopwatch.ElapsedMilliseconds
        }
    }
}

$results = New-Object System.Collections.ArrayList

function Run-TestCase {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [int[]]$ExpectedStatus = @(200),
        [hashtable]$Headers = @{},
        $Body = $null,
        [switch]$Skip
    )

    $normalizedPath = if ($Path.StartsWith("/")) { $Path } else { "/" + $Path }
    $url = $BaseUrl.TrimEnd("/") + $normalizedPath

    if ($Skip) {
        $item = [pscustomobject]@{
            Name           = $Name
            Method         = $Method
            Path           = $normalizedPath
            Url            = $url
            ExpectedStatus = ($ExpectedStatus -join ",")
            ActualStatus   = "SKIPPED"
            Outcome        = "SKIPPED"
            DurationMs     = 0
            RequestBody    = Get-JsonString -Value $Body -Depth 30
            ResponseBody   = ""
            ErrorMessage   = "Skipped due to missing prerequisite"
        }

        [void]$results.Add($item)
        Write-Host ("[SKIPPED] {0}" -f $Name) -ForegroundColor Yellow
        return $item
    }

    $response = Invoke-ApiRequest -Method $Method -Uri $url -Headers $Headers -Body $Body -TimeoutSec $TimeoutSec
    $isPass = $ExpectedStatus -contains $response.StatusCode

    $item = [pscustomobject]@{
        Name           = $Name
        Method         = $Method
        Path           = $normalizedPath
        Url            = $url
        ExpectedStatus = ($ExpectedStatus -join ",")
        ActualStatus   = [string]$response.StatusCode
        Outcome        = $(if ($isPass) { "PASS" } else { "FAIL" })
        DurationMs     = $response.DurationMs
        RequestBody    = Get-JsonString -Value $Body -Depth 30
        ResponseBody   = [string]$response.BodyText
        ErrorMessage   = [string]$response.ErrorMessage
    }

    [void]$results.Add($item)

    if ($isPass) {
        Write-Host ("[PASS] {0} -> {1}" -f $Name, $response.StatusCode) -ForegroundColor Green
    }
    else {
        Write-Host ("[FAIL] {0} -> expected [{1}], got [{2}]" -f $Name, ($ExpectedStatus -join ","), $response.StatusCode) -ForegroundColor Red
    }

    return $item
}

$runId = Get-Date -Format "yyyyMMddHHmmss"
$studentEmail = "student.{0}@example.com" -f $runId
$studentPassword = "SecurePass@123"
$state = "State{0}" -f $runId
$college = "College{0}" -f $runId
$branch = "Branch{0}" -f $runId
$adminEmail = "admin.{0}@example.com" -f $runId
$adminPassword = "AdminPass@123"
$testTitle = "Practice Test {0}" -f $runId

$token = $SuperAdminToken
$createdTestId = ""

Write-Host "Starting API test run..." -ForegroundColor Cyan
Write-Host ("Base URL: {0}" -f $BaseUrl)

$loginResult = Run-TestCase -Name "Admin Login (Super Admin)" -Method "POST" -Path "/admin/login" -ExpectedStatus @(200) -Body @{
    email    = $SuperAdminEmail
    password = $SuperAdminPassword
}

if ([string]::IsNullOrWhiteSpace($token) -and $loginResult.Outcome -eq "PASS") {
    try {
        $json = $loginResult.ResponseBody | ConvertFrom-Json
        if ($null -ne $json.data -and -not [string]::IsNullOrWhiteSpace($json.data.token)) {
            $token = [string]$json.data.token
        }
    }
    catch {
        $token = ""
    }
}

$hasSuperAdminToken = -not [string]::IsNullOrWhiteSpace($token)
$authHeaders = @{}
if ($hasSuperAdminToken) {
    $authHeaders["Authorization"] = "Bearer $token"
}

Run-TestCase -Name "Student Signup (Valid)" -Method "POST" -Path "/auth/signup" -ExpectedStatus @(201) -Body @{
    name          = "Student $runId"
    email         = $studentEmail
    password      = $studentPassword
    state         = $state
    college       = $college
    branch        = $branch
    yearOfPassing = 2026
} | Out-Null

Run-TestCase -Name "Student Signup (Invalid Email)" -Method "POST" -Path "/auth/signup" -ExpectedStatus @(400) -Body @{
    name          = "Invalid Email Student"
    email         = "not-an-email"
    password      = $studentPassword
    state         = $state
    college       = $college
    branch        = $branch
    yearOfPassing = 2026
} | Out-Null

Run-TestCase -Name "Student Login (Pending Account)" -Method "POST" -Path "/auth/login" -ExpectedStatus @(403) -Body @{
    email    = $studentEmail
    password = $studentPassword
} | Out-Null

Run-TestCase -Name "Student Login (Wrong Password)" -Method "POST" -Path "/auth/login" -ExpectedStatus @(401) -Body @{
    email    = $studentEmail
    password = "WrongPassword@123"
} | Out-Null

Run-TestCase -Name "Get Users (No Token)" -Method "GET" -Path "/admin/users?page=1&limit=10" -ExpectedStatus @(401) | Out-Null

Run-TestCase -Name "Get Users (Super Admin)" -Method "GET" -Path "/admin/users?page=1&limit=10" -ExpectedStatus @(200) -Headers $authHeaders -Skip:(-not $hasSuperAdminToken) | Out-Null

$addAdminResult = Run-TestCase -Name "Add Admin (Valid)" -Method "POST" -Path "/admin/add-admin" -ExpectedStatus @(201) -Headers $authHeaders -Body @{
    name     = "College Admin $runId"
    email    = $adminEmail
    password = $adminPassword
    scopes   = @(
        @{
            state   = $state
            college = $college
            branch  = $branch
        }
    )
} -Skip:(-not $hasSuperAdminToken)

Run-TestCase -Name "Add Admin (Invalid Email)" -Method "POST" -Path "/admin/add-admin" -ExpectedStatus @(400) -Headers $authHeaders -Body @{
    name     = "Invalid Admin"
    email    = "bad-email"
    password = $adminPassword
    scopes   = @(
        @{
            state   = $state
            college = $college
            branch  = $branch
        }
    )
} -Skip:(-not $hasSuperAdminToken) | Out-Null

Run-TestCase -Name "Get Admins (Filtered)" -Method "GET" -Path ("/admin/admins?page=1&limit=10&state={0}&college={1}&isActive=true" -f $state, $college) -ExpectedStatus @(200) -Headers $authHeaders -Skip:(-not $hasSuperAdminToken) | Out-Null

Run-TestCase -Name "Get Admin Credentials (Missing Branch)" -Method "GET" -Path ("/admincred?college={0}" -f $college) -ExpectedStatus @(400) -Headers $authHeaders -Skip:(-not $hasSuperAdminToken) | Out-Null

Run-TestCase -Name "Get Admin Credentials (Valid)" -Method "GET" -Path ("/admincred?college={0}&branch={1}" -f $college, $branch) -ExpectedStatus @(200) -Headers $authHeaders -Skip:(-not $hasSuperAdminToken) | Out-Null

Run-TestCase -Name "Deactivate Admin (Valid)" -Method "PUT" -Path "/admin/del-admin" -ExpectedStatus @(200) -Headers $authHeaders -Body @{
    college = $college
    branch  = $branch
} -Skip:(-not $hasSuperAdminToken) | Out-Null

Run-TestCase -Name "Deactivate Admin (Already Deactivated)" -Method "PUT" -Path "/admin/del-admin" -ExpectedStatus @(404) -Headers $authHeaders -Body @{
    college = $college
    branch  = $branch
} -Skip:(-not $hasSuperAdminToken) | Out-Null

$createTestResult = Run-TestCase -Name "Create Practice Test (Valid)" -Method "POST" -Path "/admin/test/create" -ExpectedStatus @(201) -Headers $authHeaders -Body @{
    title           = $testTitle
    type            = "PRACTICE"
    state           = $null
    college         = $null
    branch          = $null
    yearOfPassing   = $null
    scheduledWindow = $null
    durationMinutes = 30
    totalQuestions  = 1
    totalMarks      = 1
    isPublished     = $false
    questions       = @(
        @{
            text         = "What is 15% of 200?"
            options      = @("20", "25", "30", "35")
            correctIndex = 2
            explanation  = "0.15 x 200 = 30"
            topic        = "Percentages"
            difficulty   = "EASY"
        }
    )
} -Skip:(-not $hasSuperAdminToken)

if ($createTestResult.Outcome -eq "PASS") {
    try {
        $json = $createTestResult.ResponseBody | ConvertFrom-Json
        if ($null -ne $json.data -and -not [string]::IsNullOrWhiteSpace($json.data.id)) {
            $createdTestId = [string]$json.data.id
        }
    }
    catch {
        $createdTestId = ""
    }
}

Run-TestCase -Name "Create Test (Invalid Type)" -Method "POST" -Path "/admin/test/create" -ExpectedStatus @(400) -Headers $authHeaders -Body @{
    title           = "Invalid Type Test $runId"
    type            = "MOCK"
    durationMinutes = 30
    totalQuestions  = 1
    totalMarks      = 1
    questions       = @(
        @{
            text         = "Sample?"
            options      = @("A", "B", "C", "D")
            correctIndex = 0
            topic        = "General"
            difficulty   = "EASY"
        }
    )
} -Skip:(-not $hasSuperAdminToken) | Out-Null

Run-TestCase -Name "Get Tests (Search)" -Method "GET" -Path ("/admin/tests?page=1&limit=10&search={0}" -f [Uri]::EscapeDataString($testTitle)) -ExpectedStatus @(200) -Headers $authHeaders -Skip:(-not $hasSuperAdminToken) | Out-Null

if (-not [string]::IsNullOrWhiteSpace($createdTestId)) {
    Run-TestCase -Name "Get Results (Single Test)" -Method "GET" -Path ("/admin/results?page=1&limit=10&testId={0}" -f $createdTestId) -ExpectedStatus @(200) -Headers $authHeaders -Skip:(-not $hasSuperAdminToken) | Out-Null

    Run-TestCase -Name "Delete Test (Valid)" -Method "DELETE" -Path ("/admin/test/{0}" -f $createdTestId) -ExpectedStatus @(200) -Headers $authHeaders -Skip:(-not $hasSuperAdminToken) | Out-Null

    Run-TestCase -Name "Delete Test (Already Deleted)" -Method "DELETE" -Path ("/admin/test/{0}" -f $createdTestId) -ExpectedStatus @(404) -Headers $authHeaders -Skip:(-not $hasSuperAdminToken) | Out-Null
}
else {
    Run-TestCase -Name "Get Results (Single Test)" -Method "GET" -Path "/admin/results?page=1&limit=10" -ExpectedStatus @(200) -Headers $authHeaders -Skip | Out-Null
    Run-TestCase -Name "Delete Test (Valid)" -Method "DELETE" -Path "/admin/test/<missing-test-id>" -ExpectedStatus @(200) -Headers $authHeaders -Skip | Out-Null
    Run-TestCase -Name "Delete Test (Already Deleted)" -Method "DELETE" -Path "/admin/test/<missing-test-id>" -ExpectedStatus @(404) -Headers $authHeaders -Skip | Out-Null
}

$passed = @($results | Where-Object { $_.Outcome -eq "PASS" }).Count
$failed = @($results | Where-Object { $_.Outcome -eq "FAIL" }).Count
$skipped = @($results | Where-Object { $_.Outcome -eq "SKIPPED" }).Count
$total = $results.Count

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$null = New-Item -ItemType Directory -Force -Path $OutputDirectory
$txtReportPath = Join-Path $OutputDirectory ("api-test-report-{0}.txt" -f $timestamp)
$mdReportPath = Join-Path $OutputDirectory ("api-test-report-{0}.md" -f $timestamp)

$txtBuilder = New-Object System.Text.StringBuilder
[void]$txtBuilder.AppendLine("TRINETRA API TEST REPORT")
[void]$txtBuilder.AppendLine(("Run Timestamp: {0}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss")))
[void]$txtBuilder.AppendLine(("Base URL: {0}" -f $BaseUrl))
[void]$txtBuilder.AppendLine(("Total: {0} | Passed: {1} | Failed: {2} | Skipped: {3}" -f $total, $passed, $failed, $skipped))
[void]$txtBuilder.AppendLine("")

$index = 1
foreach ($result in $results) {
    [void]$txtBuilder.AppendLine(("{0}. [{1}] {2}" -f $index, $result.Outcome, $result.Name))
    [void]$txtBuilder.AppendLine(("   Method: {0}" -f $result.Method))
    [void]$txtBuilder.AppendLine(("   URL: {0}" -f $result.Url))
    [void]$txtBuilder.AppendLine(("   Expected: {0}" -f $result.ExpectedStatus))
    [void]$txtBuilder.AppendLine(("   Actual: {0}" -f $result.ActualStatus))
    [void]$txtBuilder.AppendLine(("   DurationMs: {0}" -f $result.DurationMs))

    if (-not [string]::IsNullOrWhiteSpace($result.RequestBody)) {
        [void]$txtBuilder.AppendLine("   Request Body:")
        [void]$txtBuilder.AppendLine((Get-TrimmedText -Text $result.RequestBody -MaxLength 1500))
    }

    if (-not [string]::IsNullOrWhiteSpace($result.ResponseBody)) {
        [void]$txtBuilder.AppendLine("   Response Body:")
        [void]$txtBuilder.AppendLine((Get-TrimmedText -Text $result.ResponseBody -MaxLength 2000))
    }

    if (-not [string]::IsNullOrWhiteSpace($result.ErrorMessage)) {
        [void]$txtBuilder.AppendLine(("   Error: {0}" -f $result.ErrorMessage))
    }

    [void]$txtBuilder.AppendLine("")
    $index++
}

$txtBuilder.ToString() | Out-File -FilePath $txtReportPath -Encoding UTF8

$mdBuilder = New-Object System.Text.StringBuilder
[void]$mdBuilder.AppendLine("# Trinetra API Test Report")
[void]$mdBuilder.AppendLine("")
[void]$mdBuilder.AppendLine(("- Run Timestamp: {0}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss")))
[void]$mdBuilder.AppendLine(("- Base URL: {0}" -f $BaseUrl))
[void]$mdBuilder.AppendLine(("- Total: {0}" -f $total))
[void]$mdBuilder.AppendLine(("- Passed: {0}" -f $passed))
[void]$mdBuilder.AppendLine(("- Failed: {0}" -f $failed))
[void]$mdBuilder.AppendLine(("- Skipped: {0}" -f $skipped))
[void]$mdBuilder.AppendLine("")
[void]$mdBuilder.AppendLine("## Summary")
[void]$mdBuilder.AppendLine("")
[void]$mdBuilder.AppendLine("| # | Test | Method | Endpoint | Expected | Actual | Result |")
[void]$mdBuilder.AppendLine("|---|---|---|---|---|---|---|")

for ($i = 0; $i -lt $results.Count; $i++) {
    $result = $results[$i]
    $endpointText = Escape-MarkdownTableCell -Text $result.Path
    [void]$mdBuilder.AppendLine(("| {0} | {1} | {2} | {3} | {4} | {5} | {6} |" -f ($i + 1), (Escape-MarkdownTableCell -Text $result.Name), $result.Method, $endpointText, $result.ExpectedStatus, $result.ActualStatus, $result.Outcome))
}

[void]$mdBuilder.AppendLine("")
[void]$mdBuilder.AppendLine("## Detailed Results")
[void]$mdBuilder.AppendLine("")

for ($i = 0; $i -lt $results.Count; $i++) {
    $result = $results[$i]
    [void]$mdBuilder.AppendLine(("### {0}. {1}" -f ($i + 1), $result.Name))
    [void]$mdBuilder.AppendLine("")
    [void]$mdBuilder.AppendLine(("- Result: {0}" -f $result.Outcome))
    [void]$mdBuilder.AppendLine(("- Method: {0}" -f $result.Method))
    [void]$mdBuilder.AppendLine(("- URL: {0}" -f $result.Url))
    [void]$mdBuilder.AppendLine(("- Expected Status: {0}" -f $result.ExpectedStatus))
    [void]$mdBuilder.AppendLine(("- Actual Status: {0}" -f $result.ActualStatus))
    [void]$mdBuilder.AppendLine(("- DurationMs: {0}" -f $result.DurationMs))

    if (-not [string]::IsNullOrWhiteSpace($result.RequestBody)) {
        [void]$mdBuilder.AppendLine("")
        [void]$mdBuilder.AppendLine("Request Body:")
        [void]$mdBuilder.AppendLine('```json')
        [void]$mdBuilder.AppendLine((Get-TrimmedText -Text $result.RequestBody -MaxLength 1500))
        [void]$mdBuilder.AppendLine('```')
    }

    if (-not [string]::IsNullOrWhiteSpace($result.ResponseBody)) {
        [void]$mdBuilder.AppendLine("")
        [void]$mdBuilder.AppendLine("Response Body:")
        [void]$mdBuilder.AppendLine('```json')
        [void]$mdBuilder.AppendLine((Get-TrimmedText -Text $result.ResponseBody -MaxLength 2000))
        [void]$mdBuilder.AppendLine('```')
    }

    if (-not [string]::IsNullOrWhiteSpace($result.ErrorMessage)) {
        [void]$mdBuilder.AppendLine("")
        [void]$mdBuilder.AppendLine(("Error: {0}" -f $result.ErrorMessage))
    }

    [void]$mdBuilder.AppendLine("")
}

$mdBuilder.ToString() | Out-File -FilePath $mdReportPath -Encoding UTF8

$resolvedTxt = Resolve-Path $txtReportPath
$resolvedMd = Resolve-Path $mdReportPath

Write-Host ""
Write-Host "API test run completed." -ForegroundColor Cyan
Write-Host ("Total: {0}, Passed: {1}, Failed: {2}, Skipped: {3}" -f $total, $passed, $failed, $skipped)
Write-Host ("TXT report: {0}" -f $resolvedTxt.Path)
Write-Host ("MD report:  {0}" -f $resolvedMd.Path)

[pscustomobject]@{
    BaseUrl      = $BaseUrl
    Total        = $total
    Passed       = $passed
    Failed       = $failed
    Skipped      = $skipped
    TxtReport    = $resolvedTxt.Path
    MarkdownReport = $resolvedMd.Path
}
